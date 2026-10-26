# Nhật ký thay đổi (CHANGELOG)

Tất cả các thay đổi đáng chú ý của dự án **VibeLoad** sẽ được ghi lại trong tài liệu này.  
Định dạng tuân theo chuẩn [Keep a Changelog](https://keepachangelog.com/vi/1.0.0/) và phiên bản tuân theo [Semantic Versioning](https://semver.org/lang/vi/).

---

## [1.0.0] - 2026-10-26

### 🚀 Khởi tạo phiên bản chính thức đầu tiên

#### Tính năng chính (Core Features)
- **Hoạt động 100% On-Device (Serverless)**: Tải video và âm thanh trực tiếp trên điện thoại Android, không cần máy chủ trung gian, không thu thập dữ liệu cá nhân, không quảng cáo.
- **5 Nền tảng ưu tiên**: Hỗ trợ đầy đủ phân tích và tải media từ YouTube, TikTok, Facebook, Instagram và SoundCloud thông qua engine lõi `yt-dlp 2026.10.21` tích hợp bằng `Chaquopy 16.1.0`.
- **Định dạng & Chất lượng linh hoạt**:
  - Video MP4 đa độ phân giải (360p, 480p, 720p, 1080p, 2K, 4K nếu nguồn cung cấp).
  - Tự động ghép nối luồng hình và luồng tiếng riêng biệt (DASH muxing) bằng FFmpeg native in-process (`dev.ffmpegkit-maintained 8.1.7`).
  - Âm thanh MP3 chất lượng cao (CBR 192kbps - 320kbps) và định dạng gốc M4A/Opus.
- **Tuân thủ nghiêm ngặt Android Scoped Storage**:
  - Video lưu trữ tại `Movies/VibeLoad`.
  - Âm thanh lưu trữ tại `Music/VibeLoad`.
  - Sử dụng cơ chế `MediaStore` với cờ `IS_PENDING` chuẩn xác từ Android 10 (API 29) đến Android 15 (API 35).
- **Tải nền tin cậy**:
  - Dịch vụ nền `DownloadForegroundService` kiểu `dataSync` kèm thanh tiến trình thông báo thời gian thực, hiển thị tốc độ, dung lượng và thời gian ước tính.
  - Tự động thử lại có giới hạn (tối đa 3 lần) khi mất mạng tạm thời.
  - Hỗ trợ tạm dừng, tiếp tục và hủy tác vụ.
  - Kiểm soát nghiêm ngặt 1 tác vụ tải duy nhất tại một thời điểm (không tải dồn dập, không hỗ trợ playlist hàng loạt).
- **Thư viện & Trình phát tích hợp**:
  - Quản lý tệp video và âm thanh với bộ lọc danh mục, tìm kiếm và sắp xếp.
  - Đổi tên tệp an toàn, chia sẻ qua Android Share Sheet, xóa file khỏi máy và mở bằng ứng dụng ngoài.
  - Trình phát đa phương tiện tích hợp trên nền tảng Jetpack Compose và AndroidX Media3 (ExoPlayer), hỗ trợ tua 10 giây, điều chỉnh tốc độ từ 0.5x đến 2.0x, và phát âm thanh trong nền.
- **Cài đặt & Tùy biến**:
  - Hỗ trợ 3 chế độ giao diện: Sáng, Tối và Theo hệ thống.
  - Hiển thị dung lượng trống khả dụng của thiết bị.
  - Tính năng dọn dẹp bộ nhớ đệm (cache) và xóa lịch sử tải.
  - Trình kiểm tra bản cập nhật APK tự động qua GitHub Releases API.
- **Tiện ích hệ thống**:
  - Nhận liên kết chia sẻ trực tiếp từ trình duyệt hoặc ứng dụng mạng xã hội qua intent `ACTION_SEND`.
  - Tự động nhận diện liên kết sao chép trong khay nhớ tạm (Clipboard).

#### Kiến trúc & Kỹ thuật
- **Ngôn ngữ & Nền tảng**: Kotlin 2.0.21, Jetpack Compose Material 3, Android SDK 35 (minSdk 29).
- **Kiến trúc Clean Architecture**: Phân tách rõ ràng giữa các tầng Domain, Data, Platform và Presentation.
- **Cơ sở dữ liệu**: Room 2.6.1 cho bảng lịch sử tải.
- **Bảo mật & Tối ưu hóa**: Đóng gói thư viện gốc C/C++ trực tiếp trong APK (hỗ trợ trang nhớ 16 KB và tuân thủ chính sách W^X của Android), không thực thi binary tải về từ thư mục có thể ghi.
