# THÔNG BÁO BẢN QUYỀN VÀ GIẤY PHÉP BÊN THỨ BA
## THIRD-PARTY SOFTWARE NOTICES AND LICENSES — VIBELOAD v1.0.0

VibeLoad sử dụng các thư viện phần mềm mã nguồn mở tuân thủ nghiêm ngặt các điều khoản cấp phép tương ứng:

---

### 1. yt-dlp
- **Bản quyền:** Copyright © 2020-2026 yt-dlp contributors
- **Giấy phép:** The Unlicense (Public Domain Dedication)
- **Mục đích:** Bóc tách metadata và tải các luồng video/âm thanh trực tiếp trên thiết bị.
- **Chi tiết:** https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE

---

### 2. Chaquopy (Python for Android)
- **Bản quyền:** Copyright © 2018-2026 Chaquo Ltd.
- **Giấy phép:** MIT License & 3-clause BSD License
- **Mục đích:** Cầu nối liên kết CPython runtime vào tiến trình JVM của Android.
- **Chi tiết:** https://chaquo.com/chaquopy/

---

### 3. FFmpeg & dev.ffmpegkit-maintained
- **Bản quyền:** Copyright © 2000-2026 the FFmpeg developers / Taner Sener / Community maintainers
- **Giấy phép:** GNU Lesser General Public License (LGPL) v2.1 / GNU General Public License (GPL) v3.0
- **Mục đích:** Ghép luồng âm thanh/hình ảnh không nén (remux) và chuyển đổi mã hóa MP3 (libmp3lame).
- **Lưu ý tuân thủ:**
  - FFmpegKit được liên kết động (Dynamic Linking) dưới dạng thư viện `.so` thông qua JNI.
  - VibeLoad cung cấp đầy đủ thông tin build và hướng dẫn biên dịch lại thư viện theo nghĩa vụ của giấy phép LGPL.

---

### 4. AndroidX Media3 (ExoPlayer)
- **Bản quyền:** Copyright © 2022-2026 The Android Open Source Project
- **Giấy phép:** Apache License, Version 2.0
- **Mục đích:** Trình phát đa phương tiện tích hợp chất lượng cao, tua tiến/lùi, điều chỉnh tốc độ và phát âm thanh nền.

---

### 5. Jetpack Compose, Material 3, AndroidX Libraries
- **Bản quyền:** Copyright © 2018-2026 The Android Open Source Project
- **Giấy phép:** Apache License, Version 2.0
- **Mục đích:** Xây dựng giao diện di động hiện đại, ViewModel, Scoped Storage Navigation và Room Persistence.

---

### 6. Coil (Coroutine Image Loader)
- **Bản quyền:** Copyright © 2023 Coil Contributors
- **Giấy phép:** Apache License, Version 2.0
- **Mục đích:** Tải và hiển thị ảnh thumbnail xem trước từ mạng.

---

### 7. OkHttp
- **Bản quyền:** Copyright © 2019 Square, Inc.
- **Giấy phép:** Apache License, Version 2.0
- **Mục đích:** Kết nối HTTP REST để kiểm tra bản phát hành mới trên GitHub Releases API.
