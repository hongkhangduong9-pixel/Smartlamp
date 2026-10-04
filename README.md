# Blynk Light — Điều Khiển Đèn ESP32 Siêu Tốc Qua Quick Settings Tile

Ứng dụng Android Native tối ưu hóa hiệu năng cao nhất để điều khiển hệ thống 2 đèn Relay ESP32 thông qua Blynk Cloud, cho phép điều khiển trực tiếp từ **Android Quick Settings / Control Center (Thanh cài đặt nhanh)** mà **không cần mở app**, hoạt động ngay cả khi màn hình khóa.

---

## 🎨 Giao Diện & Bố Cục Mới (Theo Yêu Cầu)

1. **Phần trên:**
   - Trạng thái kết nối ESP32 thời gian thực (🟢 Online / 🔴 Offline • Độ trễ ping ms) và nút đồng bộ nhanh.
   - **Ảnh minh hoạ / Ảnh nền tùy chỉnh** với hiệu ứng **làm mờ dần (Blur/Fade) mượt mà từ 3/4 (75%) ảnh trở xuống**, hòa quyện tinh tế vào màu nền ứng dụng.
   - Huy hiệu thống kê trạng thái các đèn đang bật.
2. **Góc phải trên cùng:**
   - **Nút bánh răng ⚙️ (Settings):** Mở bảng cài đặt chuyên sâu 3 phân mục.
3. **Phần dưới:**
   - **2 Bóng Đèn Cảm Ứng Lớn (💡 Đèn 1 & 💡 Đèn 2):**
     - Icon bóng đèn phát sáng rực rỡ với quầng sáng màu hổ phách (Amber Glow) sống động khi BẬT và mờ dịu khi TẮT.
     - Chạm trực tiếp vào bóng đèn hoặc công tắc để bật/tắt tức thì.
     - Hiển thị nhãn tên tùy chỉnh và chân Virtual Pin tương ứng (vd: Pin V0, Pin V1, Pin V2...).
   - **Thanh lối tắt Quick Settings:** Hỗ trợ thêm nhanh vào Control Center chỉ với 1 chạm.

---

## ⚙️ Các Chức Năng Trong Cài Đặt (Bánh Răng ⚙️)

Bảng cài đặt gồm 3 thẻ chức năng đầy đủ:

### 1. Edit Giao Diện & Icon App
- **Thay đổi Icon App:** Hỗ trợ đổi icon ứng dụng trực tiếp từ trong cài đặt với 4 phong cách độc đáo (Vàng Hổ Phách mặc định, Cyber Cyan Neon, Eco Emerald Green, Ambient Neon Purple) thông qua Android Activity-Alias chuẩn.
- **Đổi nền của app:** Chọn ảnh bất kỳ từ thư viện ảnh máy thông qua Android Photo Picker (`PickVisualMedia`) hoặc sử dụng ảnh minh họa mặc định.
- **Khả năng làm mờ dần (Blur/Fade) từ 3/4 ảnh trở xuống:** Thanh trượt điều chỉnh vị trí bắt đầu làm mờ (mặc định 75% = 3/4 chiều cao ảnh).
- **Khả năng Crop & căn chỉnh vị trí ảnh (Pan Y):** Dịch chuyển ảnh lên/xuống (px) để căn góc đẹp nhất.

### 2. Quản Lý Công Tắc Vật Lý & Mã Nguồn ESP32
- **Tự Động Đồng Bộ (Live Auto-Sync):** Khi người dùng bật/tắt công tắc vật lý gắn trên tường hoặc mạch ESP32, ứng dụng tự động đồng bộ (polling 1s - 5s) và cập nhật giao diện cùng Quick Settings Tile ngay lập tức kèm thông báo trực quan.
- **Mã Nguồn Mẫu ESP32 Chuẩn (Arduino C++):** Tích hợp sẵn trong mục Cài đặt kèm nút sao chép 1 chạm, hỗ trợ 2 công tắc nút nhấn cơ chống dội phím (Debounce) và 2 Relay, đồng bộ 2 chiều với Blynk Cloud.

### 3. Edit Virtual Pin
- Gán linh hoạt bất kỳ chân Virtual Pin nào (V0, V1, V2, V3, V4, V5, V6, V7, V8... hoặc nhập tùy ý).
- Tùy chỉnh tên hiển thị cho từng bóng đèn (vd: "Đèn Phòng Khách", "Đèn Ngủ"...).
- Quick Settings Tiles trong Android Control Center sẽ tự động cập nhật theo tên và chân Pin mới lưu.

### 3. Edit Connect (Kết Nối Blynk)
- Nhập **Blynk Device Auth Token** (hỗ trợ nút ẩn/hiện và dán nhanh).
- Chỉnh sửa **BLYNK_TEMPLATE_ID** (mặc định: `TMPL6mWdFodq6`, có nút sao chép).
- Chỉnh sửa **BLYNK_TEMPLATE_NAME** (mặc định: `Đèn Thông Minh`).
- Chọn máy chủ Blynk Cloud: `blynk.cloud`, `sgp1.blynk.cloud`, `fra1.blynk.cloud`, `lon1.blynk.cloud`.
- Nút **"Lưu & Kiểm Tra Kết Nối"** hiển thị độ trễ ping thực tế.

---

## ⚡ Kiến Trúc & Quick Settings Tile
- Luồng điều khiển ngầm trực tiếp: `Quick Settings Tile -> Blynk Cloud API -> ESP32 -> Relay -> 💡 Đèn`.
- **Zero Overhead:** Không chạy MainActivity, không chạy service 24/7 gây hao pin.
- **Optimistic UI:** Phản hồi 0ms trên Tile, tự rollback nếu mất kết nối.
