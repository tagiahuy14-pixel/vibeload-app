#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
VibeLoad Auto-Release Tool
Tự động đóng gói APK, tính SHA-256, tạo tag, tạo GitHub Release và upload app-release.apk.
Hoàn toàn không cần thao tác thủ công trên trình duyệt web.
"""

import os
import sys
import json
import hashlib
import re
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
    """Đọc versionName và versionCode từ build.gradle.kts"""
    if not os.path.exists(GRADLE_PATH):
        raise FileNotFoundError(f"Không tìm thấy file: {GRADLE_PATH}")

    with open(GRADLE_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    version_name_match = re.search(r'versionName\s*=\s*["\']([^"\']+)["\']', content)
    version_code_match = re.search(r'versionCode\s*=\s*(\d+)', content)

    if not version_name_match:
        raise ValueError("Không tìm thấy versionName trong app/build.gradle.kts")

    version_name = version_name_match.group(1)
    version_code = int(version_code_match.group(1)) if version_code_match else 1
    return version_name, version_code


def compute_sha256(file_path):
    """Tính mã SHA-256 của file APK"""
    sha256 = hashlib.sha256()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            sha256.update(chunk)
    return sha256.hexdigest().upper()


def get_github_token():
    """Lấy GitHub Personal Access Token từ tham số, biến môi trường hoặc file lưu trữ"""
    # 1. Từ đối số dòng lệnh
    for i, arg in enumerate(sys.argv):
        if arg in ("--token", "-t") and i + 1 < len(sys.argv):
            return sys.argv[i + 1].strip()

    # 2. Từ biến môi trường
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if token:
        return token

    # 3. Từ file .github_token
    if os.path.exists(TOKEN_FILE):
        with open(TOKEN_FILE, "r", encoding="utf-8") as f:
            t = f.read().strip()
            if t:
                return t

    # 4. Hỏi người dùng nếu chạy tương tác
    print("=" * 60)
    print("🔑 CẦN GITHUB TOKEN ĐỂ TỰ ĐỘNG TẠO RELEASE")
    print("Vui lòng nhập GitHub Personal Access Token (PAT) có quyền 'repo' hoặc 'contents:write'.")
    print("Nếu chưa có token, bạn có thể tạo nhanh tại: https://github.com/settings/tokens")
    print("=" * 60)
    user_token = input("Nhập GitHub Token: ").strip()

    if not user_token:
        print("❌ Lỗi: Bạn chưa nhập token. Không thể tiếp tục.")
        sys.exit(1)

    # Hỏi lưu token để các lần sau không cần nhập lại
    save = input("Lưu token này vào máy để lần sau tự động dùng luôn? (y/n): ").strip().lower()
    if save in ("y", "yes", "có", "c", ""):
        with open(TOKEN_FILE, "w", encoding="utf-8") as f:
            f.write(user_token)
        print(f"✅ Đã lưu token an toàn vào: {TOKEN_FILE} (được gitignore bảo vệ)")

    return user_token


def ensure_apk_built():
    """Kiểm tra và tự động biên dịch APK nếu chưa có"""
    if not os.path.exists(APK_PATH):
        print("🔨 Chưa tìm thấy tệp APK release. Bắt đầu biên dịch assembleRelease...")
        gradle_cmd = os.path.join(ROOT_DIR, "gradlew.bat" if os.name == "nt" else "gradlew")
        res = subprocess.run([gradle_cmd, "assembleRelease"], cwd=ROOT_DIR)
        if res.returncode != 0:
            print("❌ Lỗi: Biên dịch APK thất bại.")
            sys.exit(1)

    if not os.path.exists(APK_PATH):
        print(f"❌ Lỗi: Không tìm thấy file APK tại {APK_PATH}")
        sys.exit(1)


def api_request(url, method="GET", data=None, headers=None):
    """Gửi HTTP request tới GitHub API"""
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
        with urllib.request.urlopen(req) as resp:
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
    print("🚀 VIBELOAD AUTO-RELEASE TOOL")
    print("=" * 60)

    # 1. Đọc phiên bản
    version_name, version_code = get_version_info()
    tag_name = f"v{version_name}"
    print(f"📦 Phiên bản phát hiện: {version_name} (Code: {version_code})")
    print(f"🏷️ Git Tag: {tag_name}")

    # 2. Kiểm tra token
    token = get_github_token()
    auth_header = {"Authorization": f"Bearer {token}"}

    # 3. Đảm bảo APK đã được biên dịch
    ensure_apk_built()
    apk_size = os.path.getsize(APK_PATH)
    apk_size_mb = apk_size / (1024 * 1024)
    sha256 = compute_sha256(APK_PATH)
    print(f"📁 Tệp APK: app-release.apk ({apk_size_mb:.1f} MB)")
    print(f"🔒 SHA-256 Checksum: {sha256}")

    # 4. Kiểm tra hoặc tạo release trên GitHub
    print(f"\n🌐 Đang kiểm tra Release trên GitHub ({REPO_OWNER}/{REPO_NAME})...")
    get_release_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/tags/{tag_name}"
    release_data, status_code = api_request(get_release_url, headers=auth_header)

    release_id = None
    upload_url_template = None

    if status_code == 200 and isinstance(release_data, dict) and "id" in release_data:
        print(f"ℹ️ Đã tồn tại Release cho tag {tag_name} (ID: {release_data['id']}). Sẽ cập nhật tệp APK.")
        release_id = release_data["id"]
        upload_url_template = release_data["upload_url"]

        # Xóa asset cũ nếu trùng tên app-release.apk
        assets = release_data.get("assets", [])
        for asset in assets:
            if asset.get("name") == "app-release.apk":
                print(f"🗑️ Đang xóa asset cũ (ID: {asset['id']})...")
                del_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/assets/{asset['id']}"
                api_request(del_url, method="DELETE", headers=auth_header)
    else:
        print(f"✨ Đang tạo mới GitHub Release {tag_name}...")
        create_release_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases"
        release_body = f"""## VibeLoad Phiên bản {tag_name}

### 🔒 SHA-256 Checksum:
`{sha256}`

### 🚀 Tính năng & Thay đổi:
- **Hỗ trợ tải ảnh đa định dạng**: JPG/JPEG, PNG, WebP, GIF hoạt ảnh, AVIF, BMP, TIFF, HEIC, SVG vector, ICO.
- **Tự động nhận diện Magic Bytes & chống giả mạo**: Ngăn chặn lưu mã lỗi HTML/JSON giả dạng ảnh.
- **Lưu trữ chuẩn Scoped Storage**: Lưu tại thư mục công khai `Pictures/VibeLoad`, cờ `IS_PENDING` an toàn.
- **Thư viện ảnh tích hợp**: Xem ảnh, chia sẻ, đổi tên, xóa và mở thư mục.
- **Duy trì đầy đủ chức năng video & audio**: Tải MP4 full HD, DASH muxing, chuyển đổi MP3 chất lượng cao.
"""
        payload = {
            "tag_name": tag_name,
            "target_commitish": "master",
            "name": f"VibeLoad {tag_name}",
            "body": release_body,
            "draft": False,
            "prerelease": False
        }
        create_res, create_status = api_request(create_release_url, method="POST", data=payload, headers=auth_header)
        if create_status not in (200, 201):
            print(f"❌ Lỗi tạo Release (HTTP {create_status}): {create_res}")
            sys.exit(1)

        release_id = create_res["id"]
        upload_url_template = create_res["upload_url"]
        print(f"✅ Đã tạo Release thành công (ID: {release_id})!")

    # 5. Upload app-release.apk
    clean_upload_url = upload_url_template.split("{")[0] + "?name=app-release.apk"
    print(f"\n⬆️ Đang tải lên app-release.apk ({apk_size_mb:.1f} MB)... Vui lòng đợi trong giây lát...")

    with open(APK_PATH, "rb") as f:
        apk_bytes = f.read()

    upload_headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/vnd.android.package-archive",
        "Content-Length": str(len(apk_bytes))
    }
    upload_res, upload_status = api_request(clean_upload_url, method="POST", data=apk_bytes, headers=upload_headers)

    if upload_status in (200, 201):
        print("\n" + "=" * 60)
        print("🎉 TỰ ĐỘNG PHÁT HÀNH RELEASE THÀNH CÔNG RỰC RỠ!")
        print("=" * 60)
        print(f"📦 Phiên bản: {tag_name}")
        print(f"📁 Tệp APK: app-release.apk ({apk_size_mb:.1f} MB)")
        print(f"🔒 SHA-256: {sha256}")
        print(f"🔗 Trang Release: https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/tag/{tag_name}")
        print(f"📥 Link tải trực tiếp: https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/download/{tag_name}/app-release.apk")
        print("=" * 60)
        print("✅ Tất cả thiết bị cài VibeLoad sẽ tự động nhận được thông báo cập nhật mới ngay lập tức!")
    else:
        print(f"❌ Lỗi tải lên APK (HTTP {upload_status}): {upload_res}")
        sys.exit(1)


if __name__ == "__main__":
    main()
