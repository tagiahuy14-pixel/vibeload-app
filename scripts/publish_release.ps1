# VibeLoad PowerShell Release Script
param(
    [string]$Token = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir
$PythonScript = Join-Path $ScriptDir "publish_release.py"
$TokenFile = Join-Path $RootDir ".github_token"

if ($Token -eq "" -and (Test-Path $TokenFile)) {
    $Token = (Get-Content $TokenFile -Raw).Trim()
}

if ($Token -eq "" -and $env:GITHUB_TOKEN) {
    $Token = $env:GITHUB_TOKEN.Trim()
}

if ($Token -eq "") {
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "[!] CAN GITHUB PERSONAL ACCESS TOKEN (PAT)" -ForegroundColor Yellow
    Write-Host "Token can co quyen 'repo' hoac 'contents:write'." -ForegroundColor Gray
    Write-Host "Tao nhanh tai: https://github.com/settings/tokens" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    $Token = (Read-Host "Nhap GitHub Token cua ban").Trim()
    
    if ($Token -eq "") {
        Write-Host "[ERROR] Ban chua nhap token. Huy thao tac." -ForegroundColor Red
        exit 1
    }
    
    $save = (Read-Host "Luu token vao may de lan sau khong can nhap lai? (y/n)").Trim().ToLower()
    if ($save -eq "y" -or $save -eq "yes" -or $save -eq "") {
        [System.IO.File]::WriteAllText($TokenFile, $Token, (New-Object System.Text.UTF8Encoding $false))
        Write-Host "[OK] Da luu token an toan vao file .github_token" -ForegroundColor Green
    }
}

python $PythonScript --token $Token
