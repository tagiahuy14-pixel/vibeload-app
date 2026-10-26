# ĐẶC TẢ KỸ THUẬT VIBELOAD v1.0.0
## Ứng dụng Android tải Video/MP3 miễn phí hoạt động trực tiếp trên thiết bị (Serverless)

### 1. THÔNG TIN CHUNG
- **Tên sản phẩm:** VibeLoad
- **Phiên bản:** `1.0.0`
- **Mã phiên bản (Version Code):** `1`
- **Application ID:** `com.vibeload.app`
- **Hệ điều hành mục tiêu:** Android 10 (API 29) trở lên
- **Target SDK:** API 35 (Android 15)
- **Compile SDK:** API 35
- **Ngôn ngữ phát triển:** Kotlin 2.0.21 & Python 3.11 (Chaquopy 16.1.0)
- **Kiến trúc giao diện:** Jetpack Compose + Material 3
- **Kiến trúc ứng dụng:** Clean Architecture (Presentation, Domain, Data, Platform)

---

### 2. CÁC QUYẾT ĐỊNH SẢN PHẨM CỐT LÕI (27 YÊU CẦU ĐẶC TẢ)

#### FR-01. Tải một liên kết mỗi lần
- Người dùng chỉ nhập và xử lý duy nhất 1 liên kết URL tại một thời điểm.
- Chỉ cho phép 1 tác vụ tải hoạt động.
- Không tải hàng loạt (batch download), không tải toàn bộ playlist (nếu gặp link playlist sẽ cảnh báo hoặc chỉ tải video hiện tại).
- Không tạo hàng đợi song song. Lịch sử tải lưu trữ không giới hạn các file đã hoàn tất.

#### FR-02. Lựa chọn định dạng và chất lượng
- **Video:** MP4, hỗ trợ các độ phân giải khả dụng từ nguồn (360p, 480p, 720p, 1080p, 2K, 4K).
- Tự động ghép luồng video và luồng âm thanh riêng biệt bằng `dev.ffmpegkit-maintained` JNI.
- **Âm thanh:** MP3 (128 kbps, 192 kbps, 256 kbps, 320 kbps) hoặc giữ nguyên luồng âm thanh gốc (M4A, Opus) khi nguồn cung cấp.
- Hiển thị ước tính dung lượng khi nguồn có trường thông tin `filesize` hoặc `filesize_approx`.

#### FR-03. Lưu trữ chuẩn Scoped Storage MediaStore
- Video lưu trữ tại: `Movies/VibeLoad/` (MIME: `video/mp4`).
- Âm thanh lưu trữ tại: `Music/VibeLoad/` (MIME: `audio/mpeg`, `audio/mp4`, `audio/ogg`).
- Sử dụng Android MediaStore API với cờ `IS_PENDING = 1` trong suốt quá trình tải/chuyển đổi, chỉ đặt `IS_PENDING = 0` khi file hoàn tất và được xác thực toàn vẹn.
- File độc lập, không bị hệ thống xóa khi gỡ cài đặt ứng dụng. Không yêu cầu quyền `MANAGE_EXTERNAL_STORAGE`.

#### FR-04. Tải nền và Điều khiển tác vụ
- Android Foreground Service (`DownloadForegroundService`) với notification tiến trình.
- Cập nhật thời gian thực: Phần trăm (%), Tốc độ tải (KB/s, MB/s), Dung lượng đã tải/tổng dung lượng, Thời gian còn lại (ETA).
- Hỗ trợ các hành động: Tạm dừng (Pause), Tiếp tục (Resume - khi server hỗ trợ HTTP Range), Hủy (Cancel), Tự động thử lại (Retry) tối đa 3 lần khi lỗi mạng tạm thời.

#### FR-05. Phân tích liên kết và Xem trước (Preview)
- Sau khi nhập URL, hệ thống phân tích và hiển thị: Thumbnail, Tiêu đề, Tác giả/Kênh, Nền tảng nguồn, Thời lượng, Danh sách format.
- Cho phép nghe thử âm thanh hoặc xem thử video trực tiếp qua ExoPlayer khi luồng phát hỗ trợ.

#### FR-06. Thư viện và Trình phát tích hợp
- Quản lý danh sách file đã tải: Tab Video, Tab Âm thanh, Tab Lịch sử.
- Tìm kiếm theo tên file, sắp xếp (Mới nhất, Cũ nhất, Tên A-Z, Dung lượng).
- Đổi tên file (chuẩn hóa ký tự), chia sẻ file qua Android Share Sheet (`FileProvider`), mở bằng ứng dụng ngoài, xóa file (kèm hộp thoại xác nhận).
- Trình phát tích hợp AndroidX Media3 (ExoPlayer): Tua tiến/lùi 10s, điều chỉnh tốc độ phát (0.5x, 1.0x, 1.25x, 1.5x, 2.0x), hỗ trợ phát âm thanh nền.

#### FR-07. Hệ thống cập nhật ứng dụng
- Kiểm tra bản phát hành mới qua GitHub Releases API của repository công khai.
- Hiển thị phiên bản hiện tại, phiên bản mới, ghi chú phát hành tiếng Việt.
- Tải APK cập nhật vào thư mục cache và mở trình cài đặt Android PackageInstaller để người dùng chủ động xác nhận.

#### FR-08. Giao diện người dùng Material 3
- Thiết kế hiện đại, dễ thao tác 1 tay.
- 4 Tab điều hướng: Trang chủ, Đang tải, Thư viện, Cài đặt.
- 3 Chế độ giao diện: Sáng (Light), Tối (Dark), Theo hệ thống (System Default).

#### FR-09. Ngôn ngữ Tiếng Việt 100%
- Toàn bộ chuỗi hiển thị, nhãn, thông báo, lỗi, trạng thái đều sử dụng Android String Resources bằng tiếng Việt.

#### FR-10. Tương thích Android 10 trở lên
- `minSdk = 29`, `targetSdk = 35`.
- Tuân thủ toàn bộ chính sách bảo mật scoped storage, notification runtime permissions, foreground service types (`dataSync`).

#### FR-11. Công nghệ Android Native
- 100% Native Kotlin + Jetpack Compose + Coroutines/Flow + Room DB + Media3.
- Tích hợp Python qua Chaquopy 16.1.0 và yt-dlp.
- Xử lý media qua `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7` (JNI in-process).

#### FR-12. Hỗ trợ 5 nền tảng ưu tiên
- YouTube, TikTok, Facebook, Instagram, SoundCloud.
- Chỉ tải nội dung công khai hợp lệ mà người dùng có quyền tải. Không vượt DRM hoặc cơ chế bảo mật trả phí.

#### FR-13. Nhập liên kết thông minh
- Nhập tay, nút Dán từ Clipboard theo yêu cầu người dùng, nhận URL qua Android Share Sheet (`ACTION_SEND`).

#### FR-14. Tùy chọn Bitrate MP3 và âm thanh gốc
- Cho phép mã hóa MP3 với các bitrate: 128 kbps, 192 kbps, 256 kbps, 320 kbps bằng FFmpeg LAME encoder.
- Tùy chọn giữ nguyên luồng âm thanh gốc (M4A/AAC, Opus) mà không cần mã hóa lại.

#### FR-15. Xử lý lỗi toàn diện bằng Tiếng Việt
- Bắt và phân loại lỗi: URL sai, không kết nối được mạng, nguồn lỗi/riêng tư, không đủ dung lượng, lỗi xử lý ffmpeg, lỗi lưu MediaStore.

#### FR-16. Quyền riêng tư tối đa
- Không tài khoản, không máy chủ backend riêng, không quảng cáo, không analytics/telemetry, không tải dữ liệu người dùng lên đám mây.
- Dữ liệu lưu cục bộ trong Room DB và SharedPreferences. Cho phép xóa lịch sử, dọn cache, xóa file tạm an toàn.

#### FR-17 & FR-18. CI/CD & Kiến trúc 2 Repository
- Private Repository: Chứa toàn bộ source code, CI build/test workflow, tài liệu kỹ thuật.
- Public Repository: Chỉ phân phối file APK phát hành, checksum SHA-256, release notes.

#### FR-19. Nhận diện thương hiệu & Icon
- Tên: VibeLoad.
- Biểu tượng Adaptive Icon: Nút Play, Nốt nhạc, Mũi tên Download.

#### FR-20. Hỗ trợ ABI & Kích thước APK
- Release APK: Tối ưu cho `arm64-v8a`.
- Debug Emulator: Hỗ trợ `x86_64`.
- Tương thích cấu trúc trang nhớ 16 KB (Android 15+).

#### FR-21. Kết nối mạng
- Hỗ trợ cả Wi-Fi và mạng di động (4G/5G). Không bóp băng thông.

#### FR-22. Đặt tên file an toàn
- Lấy mặc định từ tiêu đề media, cho phép người dùng tùy chỉnh.
- Chuẩn hóa ký tự đặc biệt, ngăn path traversal (`../`), xử lý trùng tên bằng cách tự động đánh số `(1)`, `(2)`.

#### FR-23. Kiểm thử tự động
- Unit tests tự động kiểm thử format resolver, filename sanitizer, state reducer, update manager.
- Kiểm thử build APK và chạy trên Android Emulator.

#### FR-24 & FR-25. Thực thi Windows & Chuẩn hóa Gradle
- Thực thi toàn bộ lệnh qua Gradle Wrapper (`gradlew.bat`).
- Tuân thủ quy trình kiểm chứng 5 giai đoạn nghiêm ngặt.

#### FR-26. Git cục bộ
- Commit theo từng giai đoạn, không tự push lên remote repository khi chưa có lệnh.

#### FR-27. Hồ sơ bàn giao đầy đủ
- Đầy đủ mã nguồn, APK, SHA-256, tài liệu hướng dẫn và báo cáo kiểm thử trung thực.
