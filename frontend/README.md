# ShopFlow Frontend – React E-Commerce Demo

Giao diện thực hành đặt hàng và quan sát luồng Saga (Kafka + Outbox) trực quan.

---

## 🚀 Khởi chạy Frontend

```bash
cd frontend

# Cài đặt dependencies (nếu chưa cài)
npm install

# Chạy dev server
npm run dev
```

Ứng dụng sẽ chạy tại: **`http://localhost:3000`**

---

## 🌐 Kiến trúc kết nối qua API Gateway (Nginx)

```
Browser (http://localhost:3000)
    │
    ▼ (Vite Proxy: /api/* -> http://localhost:80)
Nginx Gateway (port 80)
    ├── /api/auth/*     ──► auth-service (8081)
    ├── /api/products/* ──► product-service (8082)
    └── /api/orders/*   ──► order-service (8083)
                             │ (Kafka: payment.commands)
                             ▼
                         payment-service (8084)
                             │ (Kafka: payment.events)
                             ▼
                         order-service (cập nhật CONFIRMED / CANCELLED)
```

---

## 🛒 Quy trình thực hành đặt hàng (Order Flow)

### 1. Đăng ký & Đăng nhập
1. Truy cập `http://localhost:3000/login`.
2. Chuyển sang tab **"Tạo tài khoản"** và nhập email / mật khẩu (tối thiểu 8 ký tự).
3. Hệ thống tự động đăng nhập và lưu JWT token vào `localStorage`.

### 2. Xem sản phẩm & Thêm vào giỏ
- Vào trang **"Sản phẩm"** (`/`).
- Các sản phẩm mẫu từ ClickHouse:
  - `prod-keyboard` ($89.00): Bàn phím cơ
  - `prod-mouse` ($29.50): Chuột không dây
  - `prod-monitor` ($349.99): Màn hình 4K
  - `prod-decline-test` ($10.13): Test kịch bản thanh toán bị từ chối (`INSUFFICIENT_FUNDS`)
  - `prod-flaky-test` ($20.14): Test kịch bản timeout / retry nhưng chỉ trừ tiền 1 lần (Idempotency)
- Bấm **"Thêm vào giỏ"**.

### 3. Đặt hàng (Idempotent Checkout)
- Mở giỏ hàng bằng nút giỏ ở thanh Navbar.
- Bấm **"Đặt hàng ngay"**.
- Request gửi tới `POST /api/orders` kèm header `Idempotency-Key` được sinh tự động bằng UUID v4, đảm bảo không bao giờ bị trừ tiền trùng lặp khi mạng bị giật hoặc ấn đúp.

### 4. Theo dõi Saga Realtime (Order Tracking)
- Ngay sau khi đặt hàng thành công, ứng dụng tự động chuyển tới trang chi tiết đơn hàng: `/orders/:id`.
- Màn hình sẽ hiển thị banner:
  > **Đang theo dõi trạng thái realtime — polling mỗi 3s**
- Trạng thái ban đầu: `PENDING` (Chờ Kafka xử lý thanh toán).
- Khi `payment-service` tiêu thụ command và phát event qua Kafka:
  - Thành công: chuyển sang `CONFIRMED` / `PAID` (màu xanh lục).
  - Thất bại (nếu chọn sản phẩm đuôi `.13`): chuyển sang `CANCELLED` (bù trừ Saga, màu đỏ).
- Khi trạng thái đạt trạng thái cuối (Terminal status), quá trình polling tự động dừng lại.

### 5. Xem lịch sử đơn hàng
- Truy cập menu **"Đơn hàng"** (`/orders`) trên Navbar để xem danh sách toàn bộ các đơn hàng đã thực hiện.
