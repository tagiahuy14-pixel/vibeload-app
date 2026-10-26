# HƯỚNG DẪN KÝ DUYỆT VÀ PHÁT HÀNH VIBELOAD v1.0.0
## Quy trình DevOps, GitHub Actions CI/CD và Bảo mật Chữ ký số APK

### 1. NGUYÊN TẮC BẢO MẬT BẮT BUỘC
- **TUYỆT ĐỐI KHÔNG** commit file keystore (`.jks`, `.keystore`) hoặc thông tin mật khẩu vào Git repository.
- Mọi hoạt động ký phát hành (Signing) trên môi trường CI/CD phải thông qua **GitHub Actions Encrypted Secrets**.
- File keystore gốc phải được sao lưu ngoại tuyến an toàn (Offline Backup) tại ít nhất hai địa điểm lưu trữ độc lập. Nếu mất khóa ký, bạn sẽ không thể phát hành các bản cập nhật tiếp theo cho cùng một Application ID `com.vibeload.app`.

---

### 2. TẠO KHÓA KÝ SỐ MỚI (RELEASE KEYSTORE)
Chỉ thực hiện khi người dùng quyết định khởi tạo khóa ký chính thức:

```powershell
keytool -genkey -v -keystore "vibeload-release.keystore" `
    -alias "vibeload" `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass "MatKhauBaoMatCuaBan" `
    -keypass "MatKhauBaoMatCuaBan" `
    -dname "CN=VibeLoad, OU=Mobile, O=VibeLoad, L=Hanoi, ST=Hanoi, C=VN"
```

---

### 3. CẤU HÌNH GITHUB ACTIONS SECRETS
Để GitHub Actions có thể tự động ký APK Release trong quy trình CI/CD, chuyển đổi file keystore sang chuỗi Base64:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("vibeload-release.keystore")) | Set-Clipboard
```

Sau đó vào trang cấu hình repository: **Settings -> Secrets and variables -> Actions -> New repository secret**, thêm các biến sau:
- `KEYSTORE_BASE64`: Nội dung chuỗi Base64 vừa copy.
- `KEYSTORE_PASSWORD`: Mật khẩu bảo vệ keystore.
- `KEY_ALIAS`: Tên alias (mặc định: `vibeload`).
- `KEY_PASSWORD`: Mật khẩu của key alias.
- `PUBLIC_REPO_TOKEN`: GitHub Personal Access Token (với quyền `contents: write`) dùng để đẩy file APK sang Public Repository (chỉ cấu hình sau khi người dùng phê duyệt).

---

### 4. QUY TRÌNH KÝ THỦ CÔNG TRÊN MÁY CỤC BỘ
Nếu cần ký thủ công một bản APK Release trước khi phân phối:

1. **Căn chỉnh cấu trúc trang nhớ (Zipalign):**
   ```powershell
   & "$env:ANDROID_HOME\build-tools\35.0.0\zipalign.exe" -v -p 4 "app-release-unsigned.apk" "app-release-aligned.apk"
   ```
2. **Ký số bằng apksigner (Hỗ trợ Signature Scheme v2, v3):**
   ```powershell
   & "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" sign `
       --ks "vibeload-release.keystore" `
       --ks-key-alias "vibeload" `
       --ks-pass "pass:MatKhauBaoMatCuaBan" `
       --key-pass "pass:MatKhauBaoMatCuaBan" `
       --out "VibeLoad-1.0.0.apk" `
       "app-release-aligned.apk"
   ```
3. **Xác minh chữ ký số:**
   ```powershell
   & "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose "VibeLoad-1.0.0.apk"
   ```

---

### 5. KIỂM TRA MÃ BĂM VÀ TẠO RELEASE ASSETS
Tạo file checksum SHA-256 đi kèm bản phát hành:

```powershell
$hash = (Get-FileHash -Algorithm SHA256 "VibeLoad-1.0.0.apk").Hash
"$hash  VibeLoad-1.0.0.apk" | Out-File -Encoding ascii "VibeLoad-1.0.0.apk.sha256"
```

---

### 6. QUY TRÌNH PHÁT HÀNH SANG PUBLIC REPOSITORY
1. Public Repository chỉ chứa:
   - File APK phát hành (`VibeLoad-1.0.0.apk`).
   - File mã băm (`VibeLoad-1.0.0.apk.sha256`).
   - Ghi chú phát hành tiếng Việt (`RELEASE_NOTES_VI.md`).
2. Tuyệt đối không sao chép mã nguồn của dự án sang Public Repository.
3. Bản cập nhật sẽ được ứng dụng VibeLoad tự động phát hiện thông qua GitHub Releases API.
