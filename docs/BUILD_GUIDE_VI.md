# HƯỚNG DẪN BUILD VÀ KIỂM THỬ VIBELOAD v1.0.0
## Dành cho lập trình viên và môi trường Windows

### 1. YÊU CẦU MÔI TRƯỜNG PHÁT TRIỂN
- **Hệ điều hành:** Windows 10 / 11 (64-bit)
- **Java Development Kit:** JDK 21 (Temurin / OpenJDK 21 LTS)
- **Android SDK:**
  - Android SDK Platform API 35 (`android-35`)
  - Android SDK Build-Tools 35.0.0 trở lên
  - NDK hỗ trợ kiến trúc arm64-v8a và x86_64
- **Công cụ dòng lệnh:** PowerShell 5.1 / 7.x, Git 2.4x+
- **Gradle:** Sử dụng Gradle Wrapper đi kèm dự án (`gradlew.bat` v8.9)

---

### 2. CẤU HÌNH MÔI TRƯỜNG BAN ĐẦU
1. **Kiểm tra biến môi trường:**
   ```powershell
   java -version
   git --version
   $env:ANDROID_HOME
   ```
2. **Cấu hình đường dẫn Android SDK (`local.properties`):**
   Tạo hoặc kiểm tra file `local.properties` tại thư mục gốc của dự án:
   ```properties
   sdk.dir=C\:\\Users\\tagia\\AppData\\Local\\Android\\Sdk
   ```
   *(Lưu ý: Ký tự hai chấm `:` cần được escape bằng `\:` trong file properties).*

---

### 3. CÁC LỆNH BUILD DỰ ÁN

#### 3.1. Dọn dẹp cache và thư mục build
```powershell
.\gradlew.bat clean
```

#### 3.2. Chạy bài kiểm thử đơn vị (Unit Tests)
```powershell
.\gradlew.bat :app:testDebugUnitTest
```
Kết quả kiểm thử HTML sẽ được xuất bản tại:
`app/build/reports/tests/testDebugUnitTest/index.html`

#### 3.3. Kiểm tra mã nguồn (Android Lint)
```powershell
.\gradlew.bat :app:lintDebug
```

#### 3.4. Build bản APK Debug
```powershell
.\gradlew.bat :app:assembleDebug
```
File APK đầu ra nằm tại:
`app/build/outputs/apk/debug/app-debug.apk`

#### 3.5. Build bản APK Release
```powershell
.\gradlew.bat :app:assembleRelease
```
File APK đầu ra nằm tại:
`app/build/outputs/apk/release/app-release-unsigned.apk` (hoặc `app-release.apk` nếu đã cấu hình keystore).

---

### 4. KIỂM TRA TÍNH TOÀN VẸN CỦA FILE APK

#### 4.1. Tạo mã băm SHA-256 (PowerShell)
```powershell
Get-FileHash -Algorithm SHA256 -Path "app\build\outputs\apk\debug\app-debug.apk"
```

#### 4.2. Kiểm tra thông tin gói APK với AAPT
```powershell
& "$env:ANDROID_HOME\build-tools\35.0.0\aapt.exe" dump badging "app\build\outputs\apk\debug\app-debug.apk" | Select-String "package:"
```

#### 4.3. Kiểm tra chữ ký số với apksigner
```powershell
& "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose "app\build\outputs\apk\debug\app-debug.apk"
```

---

### 5. CÀI ĐẶT LÊN THIẾT BỊ HOẶC EMULATOR
1. Khởi động Emulator hoặc kết nối thiết bị qua cáp USB có bật USB Debugging:
   ```powershell
   adb devices
   ```
2. Cài đặt APK:
   ```powershell
   adb install -r "app\build\outputs\apk\debug\app-debug.apk"
   ```
3. Xem nhật ký logcat lọc theo tag ứng dụng:
   ```powershell
   adb logcat -s VibeLoad:V *:E
   ```
