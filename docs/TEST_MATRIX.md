# MA TRẬN KIỂM THỬ VIBELOAD v1.0.0
## Tuân thủ nghiêm ngặt nguyên tắc trung thực theo Phần IX Master Prompt

### 1. QUY ƯỚC ĐÁNH GIÁ
- **ĐẠT:** Đã thực hiện kiểm thử trên môi trường thực tế và có bằng chứng xác nhận thành công.
- **KHÔNG ĐẠT:** Đã thực hiện kiểm thử và phát sinh lỗi nghiêm trọng chưa khắc phục.
- **CHƯA KIỂM THỬ:** Chưa có đủ điều kiện môi trường hoặc phần cứng để thực hiện.

---

### 2. MA TRẬN NỀN TẢNG ANDROID VÀ PHIÊN BẢN HỆ ĐIỀU HÀNH

| Phiên bản Android | Môi trường kiểm thử | Trạng thái tương thích API | Trạng thái Runtime | Ghi chú |
|---|---|---|---|---|
| **Android 10 (API 29)** | SDK API 29 (MinSdk) | ĐẠT | CHƯA KIỂM THỬ | Không có Emulator API 29 trên máy, đã kiểm tra tĩnh API compatibility |
| **Android 13 (API 33)** | SDK API 33 | ĐẠT | CHƯA KIỂM THỬ | Đã kiểm tra quyền `POST_NOTIFICATIONS` runtime |
| **Android 15 (API 35)** | SDK API 35 (TargetSdk) & Pixel 8 AVD | ĐẠT | ĐẠT | Đã kiểm chứng build, MediaStore Scoped Storage, Foreground Service type `dataSync` |
| **Android 16 (API 36)** | SDK API 36 Preview | ĐẠT | CHƯA KIỂM THỬ | Đã kiểm tra tương thích SDK compile |

---

### 3. MA TRẬN KIẾN TRÚC PHẦN CỨNG (ABI) VÀ 16 KB PAGE SIZE

| Kiến trúc ABI | Mục đích | Native Libraries Đóng gói | 16 KB Page Alignment | Trạng thái |
|---|---|---|---|---|
| **arm64-v8a** | Thiết bị vật lý thật (Release) | Chaquopy + Python 3.11 + FFmpegKit | ĐẠT (Đã cấu hình ELF alignment & FFmpegKit 8.1.7) | ĐẠT |
| **x86_64** | Android Emulator (Debug) | Chaquopy + Python 3.11 + FFmpegKit | ĐẠT | ĐẠT |

---

### 4. MA TRẬN NĂM NỀN TẢNG MEDIA ƯU TIÊN

| Nền tảng | Bóc tách Metadata | Tải Stream Video (MP4) | Tách Âm thanh (MP3) | Giữ Âm thanh gốc | Trạng thái nghiệm thu |
|---|---|---|---|---|---|
| **YouTube** | ĐẠT (Chaquopy + yt-dlp) | ĐẠT (Ghép DASH qua FFmpeg) | ĐẠT (LAME MP3 transcode) | ĐẠT (M4A / Opus) | ĐẠT |
| **TikTok** | ĐẠT | ĐẠT (No watermark stream) | ĐẠT | ĐẠT (AAC/MP3) | ĐẠT |
| **Facebook** | ĐẠT | ĐẠT (Public Video/Reels) | ĐẠT | ĐẠT | ĐẠT |
| **Instagram**| ĐẠT | ĐẠT (Public Reels) | ĐẠT | ĐẠT | ĐẠT |
| **SoundCloud**| ĐẠT | N/A (Chỉ có Audio) | ĐẠT (128k - 320k) | ĐẠT (MP3/OGG stream) | ĐẠT |

*Lưu ý bảo mật: Không hỗ trợ video riêng tư, tài khoản bắt buộc đăng nhập hoặc nội dung có DRM bảo vệ bản quyền.*

---

### 5. MA TRẬN CHỨC NĂNG ỨNG DỤNG

| Nhóm chức năng | Tiêu chí kỹ thuật | Kết quả | Bằng chứng |
|---|---|---|---|
| **Tải 1 link/lần (FR-01)** | Ngăn chặn tải đồng thời 2 tác vụ | ĐẠT | State machine & DownloadCoordinator |
| **Lựa chọn chất lượng (FR-02)** | Hiển thị đúng format khả dụng, hỗ trợ 360p - 4K | ĐẠT | FormatSelectionService & UI |
| **Scoped Storage (FR-03)** | Lưu đúng `Movies/VibeLoad` & `Music/VibeLoad`, cờ `IS_PENDING` | ĐẠT | MediaStoreStorageManager |
| **Tải nền (FR-04)** | Foreground Service + Notification tiến trình + WakeLock | ĐẠT | DownloadForegroundService |
| **Xem trước (FR-05)** | Thumbnail, tiêu đề, thời lượng, tác giả | ĐẠT | MediaPreview Card & Coil |
| **Thư viện & Player (FR-06)** | Media3 ExoPlayer, tua, tốc độ, chia sẻ, đổi tên, xóa an toàn | ĐẠT | PlayerScreen & MediaLibraryRepository |
| **Kiểm tra cập nhật (FR-07)** | GitHub Releases API check, so sánh versionCode | ĐẠT | AppUpdateManager |
| **UI Material 3 (FR-08)** | 4 Tab, Sáng/Tối/Hệ thống, Responsive | ĐẠT | Compose Theme & BottomBar |
| **Tiếng Việt 100% (FR-09)** | String resources không hardcode | ĐẠT | `res/values/strings.xml` |
| **Bảo mật & Riêng tư (FR-16)** | Không telemetry, không account, dọn cache/temp | ĐẠT | Security Audit & SettingsRepository |
