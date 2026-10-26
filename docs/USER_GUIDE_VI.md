# CẨM NANG HƯỚNG DẪN SỬ DỤNG VIBELOAD v1.0.0
## Ứng dụng tải Video & Âm thanh chất lượng cao trên Android

### 1. GIỚI THIỆU
VibeLoad là ứng dụng di động mã nguồn mở dành cho thiết bị Android, cho phép bạn tải video và âm thanh từ các nền tảng phổ biến (YouTube, TikTok, Facebook, Instagram, SoundCloud) trực tiếp về bộ nhớ máy mà không cần bất kỳ máy chủ trung gian nào. Ứng dụng tôn trọng tối đa quyền riêng tư: không yêu cầu đăng ký tài khoản, không chứa quảng cáo và không thu thập bất kỳ dữ liệu cá nhân nào.

---

### 2. HƯỚNG DẪN CÀI ĐẶT ỨNG DỤNG
1. Tải file cài đặt `VibeLoad-1.0.0.apk` về điện thoại của bạn.
2. Mở file APK vừa tải về từ thông báo hoặc ứng dụng **Quản lý tệp tin (File Manager)**.
3. Nếu hệ thống hiển thị thông báo chặn từ nguồn không xác định:
   - Nhấn **Cài đặt (Settings)**.
   - Bật công tắc **Cho phép từ nguồn này (Allow from this source)**.
   - Quay lại và nhấn **Cài đặt (Install)**.
4. Khi mở ứng dụng lần đầu trên Android 13 trở lên, hãy nhấn **Cho phép** khi ứng dụng yêu cầu quyền hiển thị thông báo để theo dõi tiến trình tải trên thanh trạng thái.

---

### 3. HƯỚNG DẪN SỬ DỤNG TỪNG CHỨC NĂNG

#### 3.1. Tải Video hoặc Âm thanh từ liên kết (Tab Trang chủ)
1. **Lấy liên kết media:**
   - Mở ứng dụng YouTube, TikTok, Facebook, Instagram hoặc SoundCloud.
   - Tại video hoặc bài hát muốn tải, nhấn nút **Chia sẻ (Share)** -> chọn **Sao chép liên kết (Copy Link)**.
   - *Cách khác:* Bạn cũng có thể nhấn trực tiếp biểu tượng **VibeLoad** trong danh sách ứng dụng chia sẻ của Android để chuyển thẳng liên kết sang ứng dụng.
2. **Dán liên kết và Phân tích:**
   - Mở VibeLoad, nhấn nút **Dán link** (hoặc gõ địa chỉ URL vào ô nhập).
   - Nhấn nút **Phân tích**. VibeLoad sẽ kết nối trực tiếp đến nguồn để lấy ảnh đại diện, tiêu đề, tác giả, thời lượng và các định dạng hỗ trợ.
3. **Lựa chọn định dạng và chất lượng:**
   - **Video (MP4):** Chọn độ phân giải mong muốn (360p, 480p, 720p, 1080p, 2K, 4K).
   - **Âm thanh (MP3):** Chọn mức bitrate mong muốn:
     - `128 kbps`: Dung lượng nhẹ nhất, phù hợp nghe thoại/podcast.
     - `192 kbps`: Chuẩn cân bằng giữa dung lượng và chất lượng.
     - `256 kbps`: Chất lượng cao.
     - `320 kbps`: Chất lượng âm thanh tối đa (Studio quality).
   - **Âm thanh gốc:** Giữ nguyên luồng âm thanh nguyên bản (M4A/Opus) của nguồn mà không cần nén lại.
4. **Tùy chỉnh tên file (Tùy chọn):**
   - Bạn có thể chỉnh sửa lại tên tập tin theo ý thích trước khi lưu.
5. **Bắt đầu tải:**
   - Nhấn nút **Tải xuống ngay**.

#### 3.2. Giám sát và Điều khiển tiến trình tải (Tab Đang tải)
- Ứng dụng chuyển sang màn hình **Đang tải**, hiển thị đầy đủ thông tin:
  - Tỉ lệ phần trăm (`%`).
  - Tốc độ tải thực tế (`KB/s` hoặc `MB/s`).
  - Dung lượng đã tải trên tổng dung lượng.
  - Thời gian dự kiến hoàn thành (ETA).
- Các nút điều khiển:
  - **Tạm dừng (Pause):** Tạm dừng tác vụ khi cần tiết kiệm pin hoặc đổi mạng.
  - **Tiếp tục (Resume):** Tiếp tục tải tiếp đoạn còn lại.
  - **Hủy (Cancel):** Hủy bỏ tác vụ và tự động dọn dẹp các tệp tin tải dở.
  - **Thử lại (Retry):** Thử kết nối lại nếu gặp sự cố ngắt kết nối mạng.
- **Tải nền:** Bạn có thể khóa màn hình hoặc chuyển sang ứng dụng khác; tiến trình tải vẫn chạy liên tục trong nền kèm thanh tiến trình trên bảng thông báo Android.

#### 3.3. Quản lý Thư viện và Trình phát đa phương tiện (Tab Thư viện)
- **Danh mục:** Chia làm 3 mục rõ ràng: **Video**, **Âm thanh** và **Lịch sử**.
- **Tìm kiếm & Sắp xếp:** Tìm nhanh file theo tên, sắp xếp theo Ngày tải mới nhất, Cũ nhất, Tên A-Z hoặc Dung lượng.
- **Thao tác tập tin:**
  - Nhấn vào tập tin để mở **Trình phát tích hợp**: Hỗ trợ xem video Full HD, tua nhanh 10 giây, điều chỉnh tốc độ phát (`0.5x` đến `2.0x`), phát âm thanh nền.
  - **Đổi tên:** Thay đổi tên hiển thị của file trong thư viện.
  - **Chia sẻ:** Gửi file trực tiếp qua Zalo, Messenger, Gmail, Drive... qua Android Share Sheet.
  - **Mở bằng ứng dụng khác:** Mở bằng VLC, MX Player, Google Photos...
  - **Xóa:** Xóa vĩnh viễn file khỏi bộ nhớ máy (kèm cảnh báo xác nhận an toàn).

#### 3.4. Quản lý Cài đặt & Dọn dẹp (Tab Cài đặt)
- **Chế độ giao diện:** Chuyển đổi linh hoạt giữa giao diện **Sáng (Light)**, **Tối (Dark)** hoặc **Theo hệ thống**.
- **Dọn dẹp bộ nhớ:**
  - *Xóa lịch sử tải:* Xóa danh sách ghi nhận trong ứng dụng mà không làm mất file media thật.
  - *Xóa bộ nhớ đệm (Cache):* Xóa ảnh thumbnail và file tạm mạng.
  - *Dọn file tạm:* Quét và giải phóng các phân mảnh tải dang dở do rớt mạng.
- **Kiểm tra cập nhật:** Nhấn **Kiểm tra bản cập nhật APK** để kiểm tra phiên bản mới trên GitHub Releases và tải trực tiếp gói nâng cấp.

---

### 4. THƯ MỤC LƯU TRỮ TRÊN THIẾT BỊ
Các file tải về được lưu tại các thư mục chuẩn của Android:
- **Video MP4:** Thư mục `Bộ nhớ trong/Movies/VibeLoad/`
- **Âm thanh MP3 / M4A:** Thư mục `Bộ nhớ trong/Music/VibeLoad/`

Bạn có thể tìm thấy các file này ngay lập tức trong ứng dụng quản lý tệp tin hoặc các trình phát nhạc/video mặc định của điện thoại.
