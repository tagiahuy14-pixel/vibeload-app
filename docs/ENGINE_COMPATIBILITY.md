# BÁO CÁO TƯƠNG THÍCH DOWNLOAD ENGINE VIBELOAD
## Chaquopy, yt-dlp, JavaScript EJS Runtime & FFmpegKit

### 1. BẢNG TỔNG HỢP PHIÊN BẢN CÔNG NGHỆ

| Thành phần | Phiên bản | Mục đích & Trách nhiệm | Trạng thái tương thích |
|---|---|---|---|
| **Chaquopy** | `16.1.0` | Cầu nối JVM ↔ CPython trên Android runtime | Đã kiểm chứng tương thích AGP 8.7 & Gradle 8.9 |
| **Python Runtime** | `3.11` | Môi trường thực thi yt-dlp trong Android | Đạt chuẩn arm64-v8a & x86_64 |
| **yt-dlp** | `2026.10.21` | Trích xuất metadata, phân tích formats, stream download | Đã đóng gói thành công qua wheel trong APK |
| **FFmpeg** | `dev.ffmpegkit-maintained:8.1.7` | In-process JNI media processor (Remux & MP3 encoding) | Hỗ trợ 16 KB pages & API 35 (Android 15) |
| **ExoPlayer / Media3**| `1.4.1` | Trình phát tích hợp video & audio trên thiết bị | Hỗ trợ phát nền & background notification |

---

### 2. PHÂN TÍCH CHI TIẾT CÁC THÀNH PHẦN

#### 2.1. Chaquopy & yt-dlp
- **Cơ chế hoạt động:** Chaquopy nhúng Python C-runtime vào tiến trình ứng dụng Android thông qua JNI (`libpython3.11.so`). Khi ứng dụng khởi động, `VibeLoadApp` kích hoạt `Python.start(AndroidPlatform(context))`.
- **yt-dlp Execution:** Mã nguồn yt-dlp được biên dịch sẵn thành byte-code và đóng gói trong tài nguyên ứng dụng. Quá trình bóc tách metadata (`extract_info`) và tải stream được gọi trực tiếp bằng các hàm Python trong tiến trình ứng dụng Android, không cần tiến trình con hay máy chủ phụ trợ.

#### 2.2. JavaScript Runtime & yt-dlp EJS (Đối với YouTube)
- **Thách thức:** Thuật toán giải mã chữ ký số (signature n-sig) và throttling của YouTube đôi lúc cần chạy đoạn mã JavaScript được trích xuất từ web client của YouTube.
- **Quy tắc bảo mật Android:** Trên Android 10+ (API 29+), hệ điều hành áp dụng chính sách `W^X` (Write XOR Execute) và SELinux nghiêm ngặt. Việc tải file nhị phân (binary như Node.js/QuickJS standalone) vào `filesDir` rồi gọi `chmod +x` và `Runtime.getRuntime().exec()` bị chặn hoặc tiềm ẩn nguy cơ bảo mật.
- **Giải pháp của VibeLoad:**
  - yt-dlp tích hợp sẵn bộ thông dịch JavaScript nội bộ (JsEngine) xử lý các biểu thức giải mã n-challenge tiêu chuẩn.
  - Sử dụng các client định danh YouTube Android (`android`, `ios`) trong yt-dlp extractor để lấy luồng phát trực tiếp mà không bị áp đặt JavaScript challenge của web player.
  - Các nền tảng TikTok, Facebook, Instagram, SoundCloud không yêu cầu EJS runtime, hoạt động trực tiếp qua HTTP REST/API extractor của yt-dlp.

#### 2.3. FFmpeg Processing (Ghép luồng & Chuyển đổi MP3)
- **Thay thế FFmpegKit cũ:** Dự án `com.arthenica:ffmpeg-kit` nguyên bản đã dừng bảo trì và bị gỡ bỏ khỏi Maven Central. VibeLoad chuyển sang sử dụng `dev.ffmpegkit-maintained:8.1.7`, nhánh duy trì chính thức cho Android hiện đại.
- **Ưu điểm vượt trội:**
  - Hoạt động in-process qua JNI, không vi phạm chính sách W^X của Android.
  - Hỗ trợ đầy đủ bộ mã hóa MP3 LAME (`libmp3lame`) với các bitrate tiêu chuẩn: 128k, 192k, 256k, 320k.
  - Hỗ trợ sao chép luồng không nén (remux) `copy` siêu nhanh khi ghép MP4 (video DASH stream + audio DASH stream).
  - Tương thích kích thước trang nhớ 16 KB (16 KB page size alignment) trên Android 15+.

---

### 3. KIỂM CHỨNG TƯƠNG THÍCH THEO ABI

| Kiến trúc ABI | Mục đích | Native Libraries (.so) | Trạng thái |
|---|---|---|---|
| **arm64-v8a** | Thiết bị vật lý thật (Release) | Chaquopy + Python 3.11 + FFmpegKit | Sẵn sàng cho Release APK |
| **x86_64** | Android Emulator (Debug) | Chaquopy + Python 3.11 + FFmpegKit | Sẵn sàng cho kiểm thử Emulator |

---

### 4. NĂM NỀN TẢNG ƯU TIÊN VÀ GIỚI HẠN

1. **YouTube:** Hỗ trợ video chất lượng từ 360p đến 1080p, 2K, 4K (tự động ghép video + audio qua FFmpeg). Hỗ trợ tách âm thanh MP3 và giữ định dạng M4A/Opus gốc. Không hỗ trợ video riêng tư, video yêu cầu đăng nhập hoặc nội dung có DRM bản quyền.
2. **TikTok:** Hỗ trợ video công khai không có watermark. Tách MP3 trực tiếp từ luồng âm thanh gốc.
3. **Facebook:** Hỗ trợ video và Reel công khai ở chất lượng HD / SD.
4. **Instagram:** Hỗ trợ Reels và video bài viết công khai. Không hỗ trợ tài khoản riêng tư (Private).
5. **SoundCloud:** Hỗ trợ trích xuất luồng âm thanh MP3/HQ streaming từ các bài hát công khai.
