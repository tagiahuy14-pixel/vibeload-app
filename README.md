# VibeLoad v1.0.0

<p align="center">
  <strong>Ứng dụng Android tải Video & Âm thanh miễn phí, chạy 100% trên thiết bị (On-Device, Serverless)</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-brightgreen" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue" alt="Target SDK">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-orange" alt="License">
</p>

---

## 🌟 Giới thiệu

**VibeLoad** là ứng dụng di động Android cho phép người dùng phân tích và tải trực tiếp video định dạng MP4 và âm thanh định dạng MP3 từ các nền tảng mạng xã hội phổ biến về điện thoại một cách an toàn và bảo mật.

Toàn bộ quy trình phân tích siêu dữ liệu, tải luồng đa phương tiện, chuyển đổi mã và ghép luồng đều được thực hiện **trực tiếp 100% trên phần cứng điện thoại** (On-Device, Serverless). VibeLoad không sử dụng máy chủ backend trung gian, không thu thập dữ liệu người dùng, không quảng cáo và không yêu cầu đăng ký tài khoản.

---

## 🎯 5 Quyết định Sản phẩm Cốt lõi

1. **100% On-Device & Serverless**: Không máy chủ VPS, không Firebase, không Cloud proxy.
2. **Quy tắc 1 Tác vụ duy nhất (FR-01)**: Tại một thời điểm chỉ cho phép 1 tác vụ tải duy nhất đang chạy. Không hỗ trợ tải hàng loạt hoặc tải nguyên playlist để bảo vệ tài nguyên thiết bị và tránh bị chặn IP.
3. **5 Nền tảng ưu tiên**: Tối ưu hóa phân tích và tải trên YouTube, TikTok, Facebook, Instagram và SoundCloud.
4. **Android Scoped Storage chuẩn xác**:
   - Video lưu tại thư mục hệ thống: `Movies/VibeLoad`
   - Âm thanh lưu tại thư mục hệ thống: `Music/VibeLoad`
   - Sử dụng `MediaStore` với cờ `IS_PENDING` chuẩn xác từ Android 10 đến 15.
5. **Giao diện 100% Tiếng Việt**: Toàn bộ chuỗi văn bản giao diện, thông báo lỗi và hướng dẫn sử dụng đều được bản địa hóa tự nhiên bằng Tiếng Việt.

---

## 🏗️ Kiến trúc & Công nghệ

Ứng dụng được xây dựng theo mô hình **Clean Architecture**:

- **Presentation Layer**: 
  - Jetpack Compose Material 3 với giao diện phản hồi nhanh, hỗ trợ chủ đề Sáng / Tối / Theo hệ thống.
  - StateFlow & Kotlin Coroutines quản lý luồng dữ liệu một chiều (UDF).
  - Trình phát tích hợp sử dụng **AndroidX Media3 (ExoPlayer)** hỗ trợ tua 10 giây, điều chỉnh tốc độ (0.5x - 2.0x) và phát âm thanh trong nền.
- **Platform Layer**:
  - **Chaquopy 16.1.0** nhúng Python 3.11 runtime trực tiếp vào tiến trình Android.
  - **yt-dlp 2026.10.21** làm engine phân tích và tải luồng đa phương tiện.
  - **dev.ffmpegkit-maintained 8.1.7** (FFmpeg n8.1.2) chạy in-process qua JNI để ghép luồng (DASH remuxing) và chuyển mã sang MP3 (`libmp3lame`) với trang nhớ 16 KB tương thích Android 15+.
  - **DownloadForegroundService**: Dịch vụ nền loại `dataSync` hiển thị tiến trình, tốc độ và thời gian ước tính trên Notification bar.
- **Data Layer**:
  - **Room Database 2.6.1** lưu trữ lịch sử tải về cục bộ.
  - **MediaStoreStorageManager** xuất bản tệp vào bộ nhớ chia sẻ Scoped Storage.

---

## 📱 Cấu trúc 4 Màn hình chính

1. **Trang chủ (Home)**: Nhập URL, dán liên kết nhanh từ khay nhớ tạm, nhận diện liên kết chia sẻ từ ứng dụng khác, phân tích siêu dữ liệu, xem trước thông tin và lựa chọn chất lượng tải (MP4 / MP3).
2. **Đang tải (Downloading)**: Theo dõi tiến trình tải với thanh phần trăm, tốc độ tải, dung lượng đã tải / tổng dung lượng, nút Tạm dừng, Tiếp tục và Hủy.
3. **Thư viện (Library)**: Quản lý toàn bộ tệp đã tải phân loại theo Video / Âm thanh / Lịch sử, hỗ trợ tìm kiếm, sắp xếp, đổi tên tệp, chia sẻ tệp qua Android Share Sheet và xóa tệp an toàn.
4. **Cài đặt (Settings)**: Tùy biến giao diện (Sáng/Tối/Hệ thống), xem dung lượng bộ nhớ trống, dọn dẹp cache tạm thời, kiểm tra bản cập nhật APK mới nhất qua GitHub Releases.

---

## 🚀 Hướng dẫn Build từ Source Code

### Yêu cầu môi trường
- **Hệ điều hành**: Windows 10/11, macOS hoặc Linux.
- **JDK**: Java Development Kit 21 (khuyến nghị Eclipse Adoptium Temurin 21).
- **Android SDK**: Build-Tools 35.0.0, Compile SDK 35, Min SDK 29.
- **Python**: Python 3.8 - 3.12 trên máy host (để hỗ trợ Chaquopy build script).

### Các lệnh Build chính

```bash
# Clone source code
git clone https://github.com/vibeload/vibeload-app.git
cd vibeload-app

# Chạy toàn bộ Unit Tests
./gradlew testDebugUnitTest

# Build APK Debug (hỗ trợ cả x86_64 emulator và arm64)
./gradlew assembleDebug

# Build APK Release (tối ưu hóa cho arm64-v8a)
./gradlew assembleRelease
```

Đường dẫn tệp APK sau khi build hoàn tất:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

---

## 🔐 Thông tin Bản quyền & Giấy phép

- Mã nguồn ứng dụng VibeLoad được phát hành theo giấy phép **MIT License**.
- Các thư viện phụ thuộc của bên thứ ba được ghi nhận chi tiết tại [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md).
- Dự án tôn trọng bản quyền nội dung số và các điều khoản dịch vụ của các nền tảng nguồn. VibeLoad không hỗ trợ vượt DRM hoặc truy cập trái phép nội dung trả phí.
