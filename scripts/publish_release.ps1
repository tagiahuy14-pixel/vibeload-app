# VibeLoad PowerShell Release Script
param(
    [string]$Token = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$PythonScript = Join-Path $ScriptDir "publish_release.py"

if ($Token -ne "") {
    python $PythonScript --token $Token
} else {
    python $PythonScript
}
