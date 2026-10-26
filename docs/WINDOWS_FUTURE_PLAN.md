# KẾ HOẠCH PHÁT TRIỂN VIBELOAD CHO WINDOWS (LỘ TRÌNH TƯƠNG LAI)
## VibeLoad Desktop v2.0 Roadmap

### 1. MỤC TIÊU CHIẾN LƯỢC
Sau khi phiên bản Android v1.0.0 hoàn thiện và đạt độ ổn định cao, VibeLoad sẽ được mở rộng sang nền tảng **Windows 10 / 11** theo định hướng ứng dụng desktop độc lập, giữ nguyên toàn bộ triết lý:
- Serverless (hoạt động 100% trên máy người dùng).
- Không tài khoản, không quảng cáo, không thu thập dữ liệu (Zero-telemetry).
- Mã nguồn mở, miễn phí và hỗ trợ đầy đủ tiếng Việt.

---

### 2. LỰA CHỌN CÔNG NGHỆ CHO WINDOWS
1. **Giao diện người dùng (UI):**
   - **Compose Multiplatform (Desktop):** Tái sử dụng tối đa cấu trúc Compose UI, hệ thống Theme Material 3 và các ViewModel hiện có từ bản Android.
2. **Download Engine trên Windows:**
   - Sử dụng tiến trình nhúng CPython hoặc gọi trực tiếp nhị phân `yt-dlp.exe` chính quy.
   - FFmpeg: Sử dụng `ffmpeg.exe` tĩnh (static build) để thực hiện ghép luồng và chuyển mã MP3.
3. **Quản lý lưu trữ trên Windows:**
   - Tương đương MediaStore của Android, lưu trữ chuẩn tại các thư mục:
     - Video: `C:\Users\<User>\Videos\VibeLoad\`
     - Âm thanh: `C:\Users\<User>\Music\VibeLoad\`
4. **Thông báo và Tích hợp hệ thống:**
   - Windows Toast Notifications thông qua Windows App SDK.
   - Tích hợp System Tray (thu nhỏ về khay hệ thống khi đang tải).
5. **Đóng gói phân phối:**
   - Gói cài đặt MSIX có chữ ký số hoặc file cài đặt EXE tự động (Inno Setup / WiX Toolset).
   - Tự động kiểm tra bản phát hành mới qua GitHub Releases API.
