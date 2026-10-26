# DANH SÁCH LỖI VÀ GIỚI HẠN KỸ THUẬT VIBELOAD v1.0.0
## Báo cáo trung thực và biện pháp khắc phục

### 1. CÁC GIỚI HẠN KỸ THUẬT CỐT LÕI

#### 1.1. Thách thức JavaScript Runtime (EJS) trên YouTube
- **Hiện tượng:** YouTube liên tục cập nhật thuật toán làm rối mã JavaScript (player signature challenge) nhằm cản trở các công cụ tải của bên thứ ba.
- **Biện pháp xử lý của VibeLoad:**
  - yt-dlp tích hợp sẵn bộ phân tích cú pháp JavaScript nội bộ (JsEngine) để giải mã các chuỗi n-challenge thông dụng.
  - Cấu hình extractor của yt-dlp ưu tiên luồng dữ liệu của client YouTube Android / iOS, vốn có độ ổn định cao hơn và ít bị ràng buộc bởi cơ chế challenge web.
- **Giới hạn còn lại:** Nếu YouTube thay đổi lớn cơ chế n-sig trong tương lai, một số định dạng chất lượng cao có thể yêu cầu cập nhật phiên bản yt-dlp mới hơn thông qua bản cập nhật APK của VibeLoad.

#### 1.2. Hạn chế thực thi Binary trên Android 10+ (Chính sách W^X và SELinux)
- **Hiện tượng:** Từ Android 10 (API 29+), hệ điều hành áp dụng chính sách `W^X` (Write XOR Execute). Mọi hành vi tự ý ghi file nhị phân (binary như QuickJS hoặc Node.js) vào thư mục `filesDir` rồi cấp quyền thực thi (`chmod +x`) đều bị chặn bởi SELinux.
- **Biện pháp xử lý của VibeLoad:**
  - Tuyệt đối không tải binary từ Internet về để thực thi trong thư mục dữ liệu ứng dụng.
  - Sử dụng toàn bộ thư viện native dưới dạng thư viện liên kết động (.so) được đóng gói chính quy trong thư mục `lib/<abi>/` của file APK và nạp qua JNI.
  - Sử dụng `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7` chạy in-process qua JNI thay vì gọi tiến trình binary riêng biệt.

#### 1.3. Cơ chế tiết kiệm pin của một số nhà sản xuất (OEM Battery Optimization)
- **Hiện tượng:** Một số hãng điện thoại (Xiaomi MIUI/HyperOS, Oppo ColorOS, Huawei HarmonyOS, Samsung OneUI) có cơ chế "hung hăng" kết thúc các tác vụ chạy nền (Background Tasks / Foreground Services) khi người dùng khóa màn hình quá 10 - 15 phút.
- **Biện pháp xử lý của VibeLoad:**
  - Sử dụng Android Foreground Service chuẩn kèm theo thông báo thường trực trên thanh trạng thái (`Notification`) và giữ `WakeLock` mức tối thiểu trong lúc truyền dữ liệu.
  - Tự động ghi nhớ trạng thái tác vụ tải và hỗ trợ tiếp tục (Resume) các phân đoạn HTTP Range khi người dùng mở lại ứng dụng.
  - Hướng dẫn người dùng tắt chế độ "Tối ưu hóa pin" (Disable Battery Optimization) cho VibeLoad nếu cần tải các tập tin video 4K dung lượng lớn.

#### 1.4. Nội dung có bản quyền, DRM và Nội dung Riêng tư
- Theo yêu cầu thiết kế sản phẩm (FR-12) và quy chuẩn đạo đức phần mềm, VibeLoad **tuyệt đối không triển khai** cơ chế bẻ khóa bản quyền DRM (Widevine, PlayReady), không tự động vượt rào xác thực hai lớp và không truy cập vào các video/bài viết riêng tư (Private) của người dùng khác.

---

### 2. KẾT QUẢ THEO DÕI VÀ KẾ HOẠCH NÂNG CẤP
Mọi lỗi phát sinh do nền tảng nguồn thay đổi cấu trúc API sẽ được ghi nhận và cập nhật nhanh chóng qua hệ thống phát hành bản nâng cấp APK tự động tích hợp trong ứng dụng.
