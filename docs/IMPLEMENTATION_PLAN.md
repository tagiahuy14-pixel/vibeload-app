# KẾ HOẠCH TRIỂN KHAI VIBELOAD v1.0.0
## 5 Giai đoạn thực thi theo Master Prompt

### TỔNG QUAN DỰ ÁN
- **Tên ứng dụng:** VibeLoad
- **Package ID:** `com.vibeload.app`
- **Phiên bản:** `1.0.0` (versionCode: 1)
- **Nền tảng:** Android (minSdk 29 - Android 10+, targetSdk 35 - Android 15)
- **Ngôn ngữ UI:** 100% Tiếng Việt (Android string resources)
- **Kiến trúc cốt lõi:** Clean Architecture (Presentation, Domain, Data, Platform Integration)
- **Giao diện:** Jetpack Compose + Material 3 (Hỗ trợ Light, Dark, System Default)
- **Target ABI:** `arm64-v8a` (Release), `x86_64` (Debug Emulator)
- **Storage:** Scoped Storage MediaStore (`Movies/VibeLoad`, `Music/VibeLoad`)
- **Tải nền:** Android Foreground Service kèm Media/Download Notification
- **Media Engine:** Chaquopy (Python 3.11) + yt-dlp + `dev.ffmpegkit-maintained` (FFmpeg in-process JNI)
- **Trình phát tích hợp:** AndroidX Media3 (ExoPlayer)

---

### GIAI ĐOẠN 1: KHỞI TẠO DỰ ÁN VÀ MÔI TRƯỜNG (PHASE 1)
1. **Kiểm tra môi trường Windows & Công cụ:**
   - Git, JDK 21 (Temurin), Android SDK 35, Gradle 8.9+, AVD Emulator (`Pixel_8`).
2. **Khởi tạo mã nguồn & Cấu hình Gradle:**
   - Setup Gradle Wrapper (`gradlew.bat`).
   - Cấu hình Multi-project Gradle: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`.
   - Cấu hình Plugins: AGP 8.7.2, Kotlin 2.0.21, Compose Compiler Plugin, Chaquopy 16.1.0.
   - Cấu hình ABI Filters: `arm64-v8a`, `x86_64`.
   - Cấu hình Scoped Storage, Permissions: `INTERNET`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`.
3. **Thiết lập kiến trúc thư mục:**
   - Presentation: UI Compose (Home, Downloading, Library, Settings), Navigation, ViewModels, Theme.
   - Domain: Models (MediaMetadata, DownloadTask, FormatOption), UseCases, Interfaces.
   - Data: Repositories (DownloadRepository, MediaStoreRepository, HistoryRepository, SettingsRepository), Room DB / DataStore.
   - Platform/Engine: Chaquopy/yt-dlp adapter, FFmpeg processor, NotificationController, DownloadService.
4. **Build & Test cơ sở:**
   - Chạy build Debug APK và unit test khởi đầu.
   - Kiểm tra commit Git checkpoint đầu tiên.

---

### GIAI ĐOẠN 2: KIỂM CHỨNG DOWNLOAD ENGINE (PHASE 2)
1. **Tích hợp Chaquopy & Python Runtime:**
   - Xác nhận Python 3.11 chạy trong runtime Android.
   - Đóng gói yt-dlp qua Chaquopy pip.
2. **Tích hợp Media Processing (FFmpeg):**
   - Đóng gói `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7` (hỗ trợ 16 KB pages & API 35).
   - Kiểm chứng chuyển đổi MP3 với các bitrate (128k, 192k, 256k, 320k) và ghép video/audio stream.
3. **Kiểm chứng phân tích & tải trên thiết bị:**
   - Kiểm tra phân tích metadata URL thực tế (YouTube, TikTok, Facebook, Instagram, SoundCloud).
   - Kiểm tra callback tiến trình (tốc độ, phần trăm, dung lượng) và cơ chế Hủy (Cancel).
   - Đánh giá khả năng tương thích và ghi nhận báo cáo trung thực.

---

### GIAI ĐOẠN 3: XÂY DỰNG TOÀN BỘ CHỨC NĂNG (PHASE 3)
1. **Trang chủ (Home Tab):**
   - Ô nhập link, nút dán clipboard (chủ động), xử lý Android Share Intent.
   - Phân tích link, hiển thị Thumbnail, Tiêu đề, Tác giả, Thời lượng, danh sách chất lượng video (360p - 4K), âm thanh (MP3, Audio gốc).
   - Cho phép đổi tên file (chuẩn hóa ký tự, tránh path traversal).
2. **Tab Đang tải (Downloading Tab):**
   - Hiển thị tiến trình chi tiết: %, tốc độ, dung lượng đã tải / tổng dung lượng, ETA.
   - Nút Tạm dừng (Pause), Tiếp tục (Resume), Hủy (Cancel), Thử lại (Retry).
3. **Tab Thư viện (Library Tab):**
   - Quản lý danh sách Video / Âm thanh / Lịch sử.
   - Tìm kiếm, sắp xếp, đổi tên, chia sẻ file, xóa file (kèm hộp thoại xác nhận).
   - Trình phát tích hợp Media3 (tua tiến/lùi, tốc độ phát, phát âm thanh nền).
4. **Tab Cài đặt (Settings Tab):**
   - Chủ đề: Sáng / Tối / Theo hệ thống.
   - Thông tin ứng dụng, kiểm tra cập nhật APK qua GitHub Releases API.
   - Xóa lịch sử, xóa cache, dọn file tạm.
   - Thông tin bảo mật & giấy phép mã nguồn mở.
5. **Dịch vụ tải nền (Background Download Service):**
   - Foreground Service với thông báo tiến trình Android.
   - Quản lý Scoped Storage MediaStore an toàn (`IS_PENDING`).

---

### GIAI ĐOẠN 4: KIỂM THỬ, TỐI ƯU VÀ SỬA LỖI (PHASE 4)
1. **Kiểm thử tự động:**
   - Unit tests cho Format Parser, Filename Sanitizer, State Reducer, Update Checker.
   - Android Lint & Build verification.
   - Kiểm thử runtime trên Android Emulator (Pixel 8).
2. **Tối ưu & Khả năng tương thích 16 KB:**
   - Kiểm tra ELF alignment và cấu hình build 16 KB page size.
   - Tối ưu giải phóng bộ nhớ, dọn dẹp file tạm.
   - Sửa mọi lỗi phát hiện trong quá trình test.

---

### GIAI ĐOẠN 5: BUILD APK, CI/CD VÀ BÀN GIAO (PHASE 5)
1. **Build APK Release:**
   - Build ARM64 Release APK với Proguard/R8 tối ưu.
   - Tính toán mã băm SHA-256.
2. **Thiết lập GitHub Actions CI/CD:**
   - `.github/workflows/ci.yml` (Build, Test, Lint).
   - `.github/workflows/release.yml` (Tự động build, ký, phát hành assets an toàn).
3. **Hoàn thiện tài liệu bàn giao:**
   - Đầy đủ 12 tài liệu theo đặc tả Master Prompt.
   - Báo cáo kết quả kiểm thử và báo cáo nghiệm thu cuối cùng.
   - Git checkpoint cục bộ (không push, không phát hành trái phép).
