# CHÍNH SÁCH BẢO MẬT VÀ QUYỀN RIÊNG TƯ VIBELOAD v1.0.0
## Nguyên tắc thiết kế bảo mật tối đa (Security & Privacy by Design)

### 1. NGUYÊN TẮC QUYỀN RIÊNG TƯ TUYỆT ĐỐI (ZERO-TRACKING & ZERO-TELEMETRY)
- **Không tài khoản người dùng:** Ứng dụng không yêu cầu đăng ký, đăng nhập hay bất kỳ thông tin định danh nào.
- **Không máy chủ phụ trợ (Serverless):** Toàn bộ quá trình phân tích và tải dữ liệu diễn ra trực tiếp giữa thiết bị Android và máy chủ lưu trữ tệp tin nguồn. VibeLoad không có máy chủ trung gian và không lưu lại lịch sử người dùng.
- **Không quảng cáo (No Ads):** Không tích hợp bất kỳ SDK quảng cáo nào (AdMob, Unity Ads, Facebook Audience Network...).
- **Không Analytics / Telemetry:** Không sử dụng Google Analytics, Firebase Crashlytics hay bất kỳ cơ chế thu thập dữ liệu tự động nào từ xa.

---

### 2. QUẢN LÝ QUYỀN TRUY CẬP ANDROID (PERMISSIONS RATIONALE)
Ứng dụng chỉ yêu cầu các quyền thực sự cần thiết theo nguyên tắc đặc quyền tối thiểu:

1. `android.permission.INTERNET`:
   - Dùng để kết nối trực tiếp đến trang web nguồn khi phân tích/tải media và kiểm tra bản cập nhật APK trên GitHub Releases.
2. `android.permission.ACCESS_NETWORK_STATE`:
   - Kiểm tra trạng thái kết nối mạng (Wi-Fi, 4G/5G, mất mạng) để tạm dừng hoặc tự động thử lại tác vụ tải khi lỗi mạng.
3. `android.permission.POST_NOTIFICATIONS` (Android 13+):
   - Cho phép hiển thị thanh tiến trình tải (%) và các nút điều khiển (Tạm dừng, Hủy) trên bảng thông báo hệ thống.
4. `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`:
   - Duy trì tiến trình tải khi người dùng chuyển sang ứng dụng khác hoặc tắt màn hình.
5. `android.permission.WAKE_LOCK`:
   - Giữ CPU hoạt động trong thời gian truyền tải các phân đoạn dữ liệu lớn mà không bị hệ thống ép dừng.

*Lưu ý: VibeLoad tuyệt đối không yêu cầu quyền `MANAGE_EXTERNAL_STORAGE` (quyền truy cập toàn bộ bộ nhớ thiết bị) hoặc quyền đọc danh bạ, vị trí, camera.*

---

### 3. KIỂM DUYỆT VÀ CHUẨN HÓA DỮ LIỆU ĐẦU VÀO (INPUT SANITIZATION)

#### 3.1. Xác thực liên kết (URL Validation)
- Chỉ chấp nhận các giao thức hợp lệ (`https://` và `http://`).
- Loại bỏ hoàn toàn các scheme nguy hiểm như `file://`, `content://`, `javascript:`, `data:`.
- Ngăn chặn kết nối vào dải mạng nội bộ riêng tư (Private IP: `127.0.0.1`, `10.0.0.0/8`, `192.168.0.0/16`) để phòng chống tấn công SSRF.

#### 3.2. Chuẩn hóa tên tệp tin (Filename Sanitization & Path Traversal Defense)
- Loại bỏ hoàn toàn các chuỗi điều hướng thư mục nguy hiểm: `../`, `..\`, `%2e%2e`.
- Chuẩn hóa và loại bỏ các ký tự bị cấm trên hệ điều hành Android và Windows: `\ / : * ? " < > |`.
- Ngăn chặn ghi đè tập tin bằng cơ chế tự động gắn hậu tố đánh số thứ tự: `(1)`, `(2)`, `(3)`.

---

### 4. QUẢN LÝ BỘ NHỚ AN TOÀN (SCOPED STORAGE)
- Sử dụng Android MediaStore API:
  - Video xuất bản vào `Movies/VibeLoad/`
  - Âm thanh xuất bản vào `Music/VibeLoad/`
- Áp dụng cờ `IS_PENDING = 1` trong suốt thời gian ghi file tạm, bảo đảm các ứng dụng khác không đọc phải tệp tin tải dở. Chỉ mở cờ `IS_PENDING = 0` sau khi tệp tin đã hoàn tất 100% và được kiểm tra tính toàn vẹn.

---

### 5. AN TOÀN CHỮ KÝ VÀ MÃ NGUỒN (GIT & CI/CD SECURITY)
- File keystore (`.keystore`, `.jks`) và mật khẩu signing được loại trừ hoàn toàn khỏi Git bằng `.gitignore`.
- Quy trình phát hành sử dụng GitHub Actions Secrets được mã hóa.
- Phân tách thành hai repository: Repository mã nguồn tư nhân (Private) và Repository phát hành công khai (Public).
