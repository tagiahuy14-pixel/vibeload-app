#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
VibeLoad Auto-Release Tool
Tu dong dong goi APK, tinh SHA-256, tao tag, tao GitHub Release va upload app-release.apk.
"""

import os
import sys
import json
import hashlib
import re
import time
import subprocess
import urllib.request
import urllib.error

# Ensure UTF-8 output on Windows consoles
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

REPO_OWNER = "tagiahuy14-pixel"
REPO_NAME = "vibeload-app"
ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
GRADLE_PATH = os.path.join(ROOT_DIR, "app", "build.gradle.kts")
APK_PATH = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "release", "app-release.apk")
TOKEN_FILE = os.path.join(ROOT_DIR, ".github_token")


def get_version_info():
    """Doc versionName va versionCode tu build.gradle.kts"""
    if not os.path.exists(GRADLE_PATH):
        raise FileNotFoundError(f"Khong tim thay file: {GRADLE_PATH}")

    with open(GRADLE_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    version_name_match = re.search(r'versionName\s*=\s*["\']([^"\']+)["\']', content)
    version_code_match = re.search(r'versionCode\s*=\s*(\d+)', content)

    if not version_name_match:
        raise ValueError("Khong tim thay versionName trong app/build.gradle.kts")

    version_name = version_name_match.group(1)
    version_code = int(version_code_match.group(1)) if version_code_match else 1
    return version_name, version_code


def compute_sha256(file_path):
    """Tinh ma SHA-256 cua file APK"""
    sha256 = hashlib.sha256()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            sha256.update(chunk)
    return sha256.hexdigest().upper()


def get_github_token():
    """Lay GitHub Personal Access Token tu tham so, bien moi truong hoac file luu tru"""
    # 1. Tu doi so dong lenh
    for i, arg in enumerate(sys.argv):
        if arg in ("--token", "-t") and i + 1 < len(sys.argv):
            return sys.argv[i + 1].strip().strip('\ufeff')

    # 2. Tu bien moi truong
    token = os.environ.get("GITHUB_TOKEN", "").strip().strip('\ufeff')
    if token:
        return token

    # 3. Tu file .github_token
    if os.path.exists(TOKEN_FILE):
        with open(TOKEN_FILE, "r", encoding="utf-8-sig") as f:
            t = f.read().strip().strip('\ufeff')
            if t:
                return t

    # 4. Hoi nguoi dung neu chay tuong tac
    print("=" * 60)
    print("[!] CAN GITHUB TOKEN DE TU DONG TAO RELEASE")
    print("Vui long nhap GitHub Personal Access Token (PAT) co quyen 'repo' hoac 'contents:write'.")
    print("Neu chua co token, ban co the tao nhanh tai: https://github.com/settings/tokens")
    print("=" * 60)

    try:
        user_token = input("Nhap GitHub Token: ").strip()
    except (EOFError, KeyboardInterrupt):
        print("\n[!] Khong the doc token tu ban phim qua tien trinh hien tai (Gradle Daemon).")
        print("\nBan co the cung cap Token theo 1 trong 3 cach sau:")
        print("  Cach 1 (De nhat): Chay script PowerShell truc tiep:")
        print("         .\\scripts\\publish_release.ps1 -Token <TOKEN_CUA_BAN>")
        print("  Cach 2: Luu token vao file .github_token trong thu muc goc du an.")
        print("  Cach 3: Chay lenh Gradle kem tham so:")
        print("         .\\gradlew.bat publishRelease -PgithubToken=<TOKEN_CUA_BAN>\n")
        sys.exit(1)

    if not user_token:
        print("[ERROR] Ban chua nhap token. Khong the tiep tuc.")
        sys.exit(1)

    # Hoi luu token de cac lan sau khong can nhap lai
    try:
        save = input("Luu token nay vao may de lan sau tu dong dung luon? (y/n): ").strip().lower()
        if save in ("y", "yes", "có", "c", ""):
            with open(TOKEN_FILE, "w", encoding="utf-8") as f:
                f.write(user_token)
            print(f"[OK] Da luu token vao: {TOKEN_FILE} (duoc gitignore bao ve an toan)")
    except Exception:
        pass

    return user_token


def ensure_apk_built():
    """Kiem tra va tu dong bien dich APK neu chua co"""
    if not os.path.exists(APK_PATH):
        print("[INFO] Chua tim thay tep APK release. Bat dau bien dich assembleRelease...")
        gradle_cmd = os.path.join(ROOT_DIR, "gradlew.bat" if os.name == "nt" else "gradlew")
        res = subprocess.run([gradle_cmd, "assembleRelease"], cwd=ROOT_DIR)
        if res.returncode != 0:
            print("[ERROR] Bien dich APK that bai.")
            sys.exit(1)

    if not os.path.exists(APK_PATH):
        print(f"[ERROR] Khong tim thay file APK tai {APK_PATH}")
        sys.exit(1)


def api_request(url, method="GET", data=None, headers=None):
    """Gui HTTP request toi GitHub API"""
    req_headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "VibeLoad-Auto-Release-Tool"
    }
    if headers:
        req_headers.update(headers)

    req_data = None
    if data is not None:
        if isinstance(data, (dict, list)):
            req_data = json.dumps(data).encode("utf-8")
            req_headers["Content-Type"] = "application/json"
        elif isinstance(data, bytes):
            req_data = data

    req = urllib.request.Request(url, data=req_data, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            content = resp.read()
            if content:
                try:
                    return json.loads(content.decode("utf-8")), resp.status
                except Exception:
                    return content, resp.status
            return None, resp.status
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="ignore")
        return {"error": str(e), "body": err_body, "code": e.code}, e.code
    except Exception as e:
        return {"error": str(e)}, 500


def main():
    print("=" * 60)
    print("[VibeLoad] CONG CU TU DONG PHAT HANH RELEASE")
    print("=" * 60)

    # 1. Doc phien ban
    version_name, version_code = get_version_info()
    tag_name = f"v{version_name}"
    print(f"[INFO] Phien ban ung dung: {version_name} (Code: {version_code})")
    print(f"[INFO] Git Tag: {tag_name}")

    # 2. Kiem tra token
    token = get_github_token()
    auth_header = {"Authorization": f"Bearer {token}"}

    # 3. Dam bao APK da duoc bien dich
    ensure_apk_built()
    apk_size = os.path.getsize(APK_PATH)
    apk_size_mb = apk_size / (1024 * 1024)
    sha256 = compute_sha256(APK_PATH)
    print(f"[INFO] Tep APK: app-release.apk ({apk_size_mb:.1f} MB)")
    print(f"[INFO] SHA-256 Checksum: {sha256}")

    # 4. Kiem tra hoac tao release tren GitHub
    print(f"\n[INFO] Dang ket noi GitHub ({REPO_OWNER}/{REPO_NAME})...")
    get_release_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/tags/{tag_name}"
    release_data, status_code = api_request(get_release_url, headers=auth_header)

    release_id = None
    upload_url_template = None

    release_body = f"""## VibeLoad Phiên bản {tag_name}

### SHA-256 Checksum:
`{sha256}`

### Tính năng & Sửa lỗi nổi bật:
- 🚀 **Cập nhật Biểu tượng Logo mới (App Icon)**:
  - Thiết kế logo nhận diện thương hiệu hoàn toàn mới: Gradient chuyển sắc xanh dương - tím điện tử hiện đại, điểm nhấn họa tiết ruy-băng 3D cách điệu Play/Download.
  - Hỗ trợ đầy đủ Adaptive Icons (Biểu tượng thích ứng) và toàn bộ bộ phân giải Mipmap (MDPI đến XXXHDPI).
  - Tương thích và bo viền chuẩn xác trên mọi giao diện launcher: Xiaomi HyperOS (Xiaomi 15 Ultra, 17 Ultra, 18 Pro/Max), Samsung OneUI (S25 Ultra, S26, S26 Ultra), Oppo ColorOS, Vivo OriginOS và Pixel Launcher.
- 📱 **Sửa lỗi hiển thị & Tối ưu bố cục thích ứng đa màn hình**:
  - Khắc phục triệt để lỗi viên thuốc lọc *"Ảnh"* bị co ép thành dải dọc kéo dài trên màn hình tiêu chuẩn hoặc khi bật tỷ lệ phóng to chữ/hiển thị của máy.
  - Bổ sung cuộn ngang mượt mà cho hàng bộ lọc thư viện (*Tất cả, Video, Âm thanh, Ảnh*), đảm bảo không bao giờ bị co rúm, méo chữ trên bất kỳ kích cỡ điện thoại nào.
  - Tối ưu căn chỉnh hàng phím tắt nền tảng (YouTube, TikTok, Facebook, Instagram, X) đều đặn, chống tràn viền.
- 🎨 **Đồng bộ hoàn hảo giao diện Sáng / Tối**:
  - Thanh Bottom Bar và System Bars (thanh trạng thái & thanh điều hướng cử chỉ) chuyển màu chính xác theo theme hệ thống hoặc chế độ người dùng chọn.
  - Sửa dứt điểm lỗi nhãn *"Tệp đã tải về"* bị tàng hình trong chế độ sáng.
- 🖼️ **Tải Album ảnh TikTok & Chọn lọc ảnh tiện lợi**:
  - Tự động nhận diện bài viết album ảnh TikTok (`vt.tiktok.com`, `tiktok.com/@user/photo/...`).
  - Hỗ trợ xem lướt album và đánh dấu chọn từng ảnh (1, 3, 5, 7...) để tải nhanh về máy kèm nhạc nền gốc.
- 📲 **Cập nhật OTA mượt mà**: Tự động thông báo và cập nhật tức thì.
"""

    if status_code == 200 and isinstance(release_data, dict) and "id" in release_data:
        print(f"[INFO] Da ton tai Release cho tag {tag_name} (ID: {release_data['id']}). Dang cap nhat tep APK...")
        release_id = release_data["id"]
        upload_url_template = release_data["upload_url"]

        # Cap nhat thong tin Release body neu can
        update_release_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/{release_id}"
        api_request(update_release_url, method="PATCH", data={"body": release_body, "name": f"VibeLoad {tag_name}"}, headers=auth_header)

        # Xoa asset cu neu trung ten app-release.apk
        assets = release_data.get("assets", [])
        for asset in assets:
            if asset.get("name") == "app-release.apk":
                print(f"[INFO] Dang xoa file APK cu de thay the (Asset ID: {asset['id']})...")
                del_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/assets/{asset['id']}"
                api_request(del_url, method="DELETE", headers=auth_header)
    else:
        print(f"[INFO] Dang tao moi GitHub Release {tag_name}...")
        create_release_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases"
        payload = {
            "tag_name": tag_name,
            "target_commitish": "main",
            "name": f"VibeLoad {tag_name}",
            "body": release_body,
            "draft": False,
            "prerelease": False
        }
        create_res, create_status = api_request(create_release_url, method="POST", data=payload, headers=auth_header)
        if create_status not in (200, 201):
            print(f"[ERROR] Loi tao Release (HTTP {create_status}): {create_res}")
            sys.exit(1)

        release_id = create_res["id"]
        upload_url_template = create_res["upload_url"]
        print(f"[OK] Da tao Release thanh cong tren GitHub (ID: {release_id})!")

    # 5. Upload app-release.apk
    clean_upload_url = upload_url_template.split("{")[0] + "?name=app-release.apk"
    print(f"\n[UPLOAD] Dang tai len app-release.apk ({apk_size_mb:.1f} MB)... Vui long doi...")

    upload_res = None
    upload_status = None

    curl_available = False
    try:
        subprocess.run(["curl.exe", "--version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        curl_available = True
    except Exception:
        curl_available = False

    for attempt in range(1, 4):
        if attempt > 1:
            print(f"[RETRY] Thu tai len lai lan {attempt}/3...")
            time.sleep(3)

        if curl_available:
            cmd = [
                "curl.exe",
                "-s", "-S",
                "-X", "POST",
                "-H", f"Authorization: Bearer {token}",
                "-H", "Content-Type: application/vnd.android.package-archive",
                "--data-binary", f"@{APK_PATH}",
                clean_upload_url
            ]
            res = subprocess.run(cmd, capture_output=True, text=True)
            if res.returncode == 0:
                try:
                    upload_res = json.loads(res.stdout)
                    if "id" in upload_res or "browser_download_url" in upload_res:
                        upload_status = 201
                        break
                    elif "errors" in upload_res:
                        upload_status = 422
                except Exception:
                    upload_status = 200
                    break
            else:
                upload_res = {"error": res.stderr}
                upload_status = 500
        else:
            with open(APK_PATH, "rb") as f:
                apk_bytes = f.read()
            upload_headers = {
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/vnd.android.package-archive",
                "Content-Length": str(len(apk_bytes))
            }
            upload_res, upload_status = api_request(clean_upload_url, method="POST", data=apk_bytes, headers=upload_headers)
            if upload_status in (200, 201):
                break

        print(f"[WARN] Lan {attempt} that bai (HTTP {upload_status}): {upload_res.get('error') if isinstance(upload_res, dict) else upload_res}")

    if upload_status in (200, 201):
        print("\n" + "=" * 60)
        print("[SUCCESS] TU DONG PHAT HANH RELEASE THANH CONG!")
        print("=" * 60)
        print(f"Phien ban: {tag_name}")
        print(f"Tep APK: app-release.apk ({apk_size_mb:.1f} MB)")
        print(f"SHA-256: {sha256}")
        print(f"Link Release: https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/tag/{tag_name}")
        print(f"Link Tai truc tiep: https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/download/{tag_name}/app-release.apk")
        print("=" * 60)
        print("[OK] Tat ca thiet bi cai VibeLoad se tu dong nhan duoc thong bao cap nhat moi!")
    else:
        print(f"[ERROR] Loi tai len APK (HTTP {upload_status}): {upload_res}")
        sys.exit(1)


if __name__ == "__main__":
    main()
