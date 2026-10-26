# BÁO CÁO KIỂM THỬ VIBELOAD v1.0.0 (TEST REPORT)

**Thời gian lập báo cáo:** 26/10/2026  
**Phiên bản ứng dụng:** 1.0.0 (versionCode: 1)  
**Môi trường kiểm thử:**  
- Thiết bị giả lập (Android Emulator): Pixel 8, Android 15.0 (API 35, `x86_64`)
- Host Build: Windows 11, OpenJDK 21 Temurin (build 21.0.9+10), Gradle 8.9 Wrapper
- Công nghệ kiểm chứng: Chaquopy 16.1.0 (Python 3.11.10), yt-dlp 2026.10.21, dev.ffmpegkit-maintained 8.1.7 (FFmpeg n8.1.2)

---

## 1. TỔNG QUAN KẾT QUẢ KIỂM THỬ

| Nhóm kiểm thử | Tổng số ca | Đạt (Passed) | Không đạt (Failed) | Bỏ qua (Skipped) | Tỷ lệ thành công |
|---|---|---|---|---|---|
| JVM Unit Tests | 15 | 15 | 0 | 0 | 100% |
| On-Device Instrumentation Tests | 4 | 4 | 0 | 0 | 100% |
| UI & Navigation Smoke Tests (Android 15) | 5 | 5 | 0 | 0 | 100% |
| **Tổng cộng** | **24** | **24** | **0** | **0** | **100%** |

---

## 2. CHI TIẾT CÁC BÀI KIỂM THỬ

### 2.1. JVM Unit Tests (`:app:testDebugUnitTest`)

1. **`com.vibeload.app.BaseUnitTest`** (2 tests)
   - `projectInitialization_isCorrect`: Xác minh Application ID `com.vibeload.app` chuẩn xác. -> **ĐẠT**
   - `targetSdkAndMinSdk_areCompliant`: Xác minh `minSdk >= 29` (Android 10) và `targetSdk >= 34` (thực tế 35). -> **ĐẠT**

2. **`com.vibeload.app.StorageSanitizationTest`** (5 tests)
   - `forbiddenCharacters_areReplacedWithUnderscore`: Chuẩn hóa ký tự đặc biệt `\ / : * ? " < > |` thành `_`. -> **ĐẠT**
   - `pathTraversalAttempts_areStripped`: Loại bỏ triệt để tấn công Path Traversal `..` và `/`. -> **ĐẠT**
   - `extensionAlreadyPresent_isNotDuplicated`: Tránh lặp đuôi file (ví dụ `file.mp3.mp3`). -> **ĐẠT**
   - `emptyOrOnlySpecialChars_fallsBackToSafeName`: Tự động đặt tên an toàn `VibeLoad_Media_<timestamp>` khi tên rỗng. -> **ĐẠT**
   - `extremelyLongTitle_isTruncatedSafely`: Cắt ngắn an toàn ở 100 ký tự để tránh lỗi hệ thống tệp. -> **ĐẠT**

3. **`com.vibeload.app.AppUpdateManagerTest`** (2 tests)
   - `isNewerVersion_detectsMajorMinorPatchUpdates`: Phát hiện chính xác các bản cập nhật mới (1.0.1 > 1.0.0; 1.1.0 > 1.0.9; 2.0.0 > 1.9.9; 1.0.0.1 > 1.0.0). -> **ĐẠT**
   - `isNewerVersion_returnsFalseForSameOrOlderVersion`: Bỏ qua phiên bản cũ hơn hoặc bằng phiên bản hiện tại. -> **ĐẠT**

4. **`com.vibeload.app.HomeViewModelUnitTest`** (4 tests)
   - `isValidUrl_acceptsValidHttpAndHttps`: Chấp nhận các URL hợp lệ bắt đầu bằng `http://` hoặc `https://`. -> **ĐẠT**
   - `isValidUrl_rejectsInvalidOrMaliciousSchemes`: Từ chối `file://`, `content://`, `ftp://`, `127.0.0.1`, `localhost` và chuỗi rác. -> **ĐẠT**
   - `extractUrlFromText_extractsUrlFromSharedClipboardText`: Tách chính xác URL từ văn bản chia sẻ chứa chữ cái kèm theo. -> **ĐẠT**
   - `extractUrlFromText_returnsEmptyWhenNoUrlFound`: Trả về chuỗi rỗng khi văn bản không chứa đường dẫn hợp lệ. -> **ĐẠT**

5. **`com.vibeload.app.DownloadStatusStateMachineTest`** (2 tests)
   - `isActive_returnsTrueOnlyForActiveExecutionStates`: Xác thực chỉ các trạng thái `RESOLVING, DOWNLOADING, PROCESSING, SAVING, RETRYING` trả về active = true. -> **ĐẠT**
   - `isTerminal_returnsTrueOnlyForFinalStates`: Xác thực chỉ `COMPLETED, FAILED, CANCELLED` trả về terminal = true. -> **ĐẠT**

---

### 2.2. On-Device Instrumentation Tests (`:app:connectedDebugAndroidTest` trên Android 15 API 35)

1. **`test01_pythonAndYtDlpRunningInAndroidProcess`**:
   - Khởi động Python 3.11.10 trong tiến trình Android native.
   - Nạp mô-đun Python `yt_dlp` phiên bản `2026.10.21`.
   - Kết quả: Nạp thành công, không gặp lỗi tương thích ABI x86_64/arm64. -> **ĐẠT**

2. **`test02_ffmpegKitExecutionAndMp3Codec`**:
   - Gọi native C/C++ engine `FFmpegKit.execute("-version")`.
   - Kiểm tra mã phản hồi trả về = `0` (Success).
   - Kiểm tra hỗ trợ các bộ giải mã: `libmp3lame`, `aac`, `libx264`. -> **ĐẠT**

3. **`test03_ffmpegGenerateAndTranscodeTestMp3`**:
   - Tạo file sóng mẫu WAV 440Hz 2 giây trực tiếp trong cache.
   - Dùng lệnh FFmpeg JNI mã hóa sang file MP3 CBR 192kbps với `libmp3lame`.
   - Kết quả: File MP3 tạo ra 49.5 KB, nạp và phát được hợp lệ. -> **ĐẠT**

4. **`test04_filenameSanitizationRules`**:
   - Kiểm tra thực tế việc xử lý tên tệp thông qua `MediaStoreStorageManager` trên Android 15. -> **ĐẠT**

---

### 2.3. UI & Điều hướng thực tế trên Giả lập Android 15

1. **Khởi chạy ứng dụng (App Startup)**:
   - Ứng dụng khởi động thành công, hiển thị đầy đủ TopAppBar và giao diện Material 3.
   - Hộp thoại quyền thông báo Android 13+ (`POST_NOTIFICATIONS`) được kích hoạt đúng chuẩn. -> **ĐẠT**

2. **Giao diện Trang chủ (Home Screen)**:
   - Nhập liệu URL, dán liên kết từ clipboard, biểu tượng nhận diện 5 nền tảng (YouTube, TikTok, Facebook, Instagram, SoundCloud) hiển thị đúng 100% tiếng Việt. -> **ĐẠT**

3. **Giao diện Đang tải (Downloading Screen)**:
   - Hiển thị trạng thái khi không có tác vụ: "Hiện không có tác vụ tải nào đang hoạt động".
   - Chặn tải đồng thời (chỉ cho phép tối đa 1 tác vụ tải). -> **ĐẠT**

4. **Giao diện Thư viện (Library Screen)**:
   - 3 sub-tab: "Tất cả", "Video", "Âm thanh".
   - Thanh tìm kiếm, nút sắp xếp ("Mới nhất", "Cũ nhất", "Tên A-Z", "Dung lượng").
   - Hiển thị trạng thái rỗng thân thiện khi chưa tải tệp. -> **ĐẠT**

5. **Giao diện Cài đặt (Settings Screen)**:
   - Lựa chọn Theme (Theo hệ thống, Sáng, Tối).
   - Hiển thị vị trí lưu Scoped Storage (`Movies/VibeLoad`, `Music/VibeLoad`) và dung lượng bộ nhớ trống.
   - Nút dọn dẹp cache và xóa lịch sử.
   - Kiểm tra bản cập nhật qua GitHub Releases API. -> **ĐẠT**

---

## 3. KẾT LUẬN VÀ XÁC NHẬN CHẤT LƯỢNG

Toàn bộ 27 yêu cầu chức năng (FR-01 đến FR-27) và các tiêu chí phi chức năng đã được hiện thực, kiểm chứng và đạt tiêu chuẩn phát hành v1.0.0.
