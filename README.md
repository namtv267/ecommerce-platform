# ecommerce-platform

Java 21 · Spring Boot 3.3 · Gradle (Kotlin DSL, multi-module) · DDD + Hexagonal · Kafka · Postgres · ClickHouse (MyBatis) · Redis

## Kiến trúc

```
                       JWT (HS256)
 client ──► auth-service :8081  (Postgres auth_db, Redis: refresh token + blacklist)
   │
   ├─► product-service :8082    (ClickHouse + MyBatis)  ◄── REST nội bộ ──┐
   │                                                                      │
   └─► order-service :8083 ───────────────────────────────────────────────┘
            │  (Postgres order_db, Redis idempotency)        SAGA ORCHESTRATOR
            │  outbox                                            ▲ outbox
            ▼                                                    │
       [payment.commands] ──────► payment-service :8084 ──► [payment.events]
                                  (Postgres payment_db, mock gateway)
```

### Saga (orchestration, order-service là orchestrator)

| Bước | Ai | Việc |
|---|---|---|
| 1 | order | `POST /api/orders` → ghi `orders(PENDING)` + outbox `ProcessPayment` **cùng 1 transaction** |
| 2 | outbox relay | đẩy `ProcessPayment` lên `payment.commands` (key = orderId) |
| 3 | payment | tạo `payments(PENDING)` → gọi gateway → ghi kết quả + outbox + inbox **cùng 1 transaction** |
| 4 | order | nhận `PaymentCompleted` → `CONFIRMED`; nhận `PaymentFailed` → `CANCELLED` (bù trừ) |
| 5 | order (job) | đơn `PENDING` quá 60s → gửi lại lệnh (cùng idempotency key), quá 3 lần → `CANCELLED` |

### Idempotency

**Tầng API (order-service)** – header `Idempotency-Key` bắt buộc
1. Redis `SET NX` giữ chỗ key (`IN_PROGRESS`, TTL 60s) → chỉ 1 request chạy, request song song nhận `409 + Retry-After`.
2. Xong thì lưu `COMPLETED` + response (TTL 24h) → request lặp nhận lại đúng response, header `Idempotent-Replayed: true`.
3. Cùng key khác nội dung (so fingerprint SHA-256 của items đã chuẩn hoá) → `422`.
4. Redis chỉ là lớp tối ưu, fail-open. Chốt chặn thật là `UNIQUE (customer_id, idempotency_key)` trong Postgres.

**Tầng Payment** – 4 lớp (`ProcessPaymentService`)
1. **Inbox** `processed_messages`: cùng `message-id` Kafka giao lại → bỏ qua.
2. **Unique `idempotency_key` + `order_id`**: lệnh gửi lại với message-id mới → không gọi gateway nếu đã có kết quả, chỉ phát lại kết quả cũ.
3. **Idempotency key gửi sang gateway**: crash/timeout giữa chừng, retry không trừ tiền 2 lần.
4. **`UPDATE ... WHERE status='PENDING'`**: hai luồng cùng quyết định thì chỉ một bên thắng.

Không giữ transaction DB trong lúc gọi gateway; PENDING được commit trước, kết quả+outbox+inbox commit cùng nhau sau.

**Tầng Consumer (order)**: inbox + state machine idempotent (`confirm()` lần 2 là no-op).

### Kịch bản test của mock gateway (theo phần xu của số tiền)
| Phần xu | Hành vi |
|---|---|
| `.13` | bị từ chối `INSUFFICIENT_FUNDS` → đơn `CANCELLED` |
| `.14` | **đã trừ tiền** nhưng mất phản hồi ở lần gọi đầu → retry cùng key vẫn chỉ trừ 1 lần → `CONFIRMED` |
| khác | thành công (+ có thể cấu hình `failure-rate`, `timeout-rate`, `latency-ms`) |

Sản phẩm seed: `prod-keyboard` 89.00, `prod-mouse` 29.50, `prod-monitor` 349.99, `prod-hub` (hết hàng), `prod-decline-test` 10.13, `prod-flaky-test` 20.14.

## Cấu trúc code (mỗi service)
```
domain/        model + exception (Java thuần)
application/   port/in (use case) · port/out (SPI) · service (điều phối, không annotation Spring)
adapter/in/    web · messaging (Kafka listener) · scheduler
adapter/out/   persistence (JDBC/JPA/MyBatis) · messaging (outbox) · gateway · catalog (REST client)
config/        wiring Spring
```
`libs/contracts` (event/command + tên topic), `libs/common-security` (xác thực JWT dùng chung, kiểm tra blacklist Redis),
`libs/messaging` (Outbox writer/relay, inbox, DLT error handler, khai báo topic).

## Chạy local

```bash
# 1. tạo Gradle wrapper một lần (cần Gradle cài sẵn)
gradle wrapper --gradle-version 8.10.2

# 2. chạy tất cả bằng Docker (build image từng service bằng Dockerfile ở thư mục gốc)
docker compose up --build

# 3. demo end-to-end (cần curl + jq)
./scripts/demo.sh
```
Hoặc chỉ chạy hạ tầng và chạy service từ IDE:
```bash
docker compose up -d postgres redis kafka clickhouse kafka-ui
./gradlew :services:auth-service:bootRun      # 8081
./gradlew :services:product-service:bootRun   # 8082
./gradlew :services:order-service:bootRun     # 8083
./gradlew :services:payment-service:bootRun   # 8084
```
Kafka UI: http://localhost:8090 (xem `payment.commands`, `payment.events`, các topic `.DLT`).
Request mẫu: `http/auth.http`, `http/order-flow.http`.

## Test
```bash
./gradlew test
```
Unit test thuần (không cần Docker), đáng đọc nhất:
- `payment-service/.../ProcessPaymentServiceIdempotencyTest` – giao lại message, gửi lại lệnh message-id mới, timeout sau khi trừ tiền, từ chối, dùng sai key.
- `order-service/.../OrderServiceTest`, `PaymentSagaServiceTest` – replay theo key, race, bù trừ, retry, payment đến muộn.

## Giới hạn đã biết (cố ý để đơn giản)
- Tồn kho chỉ kiểm tra lúc đặt, **không giữ chỗ/trừ kho** (ClickHouse không phù hợp cho dữ liệu giao dịch).
- Đơn bị huỷ do timeout mà tiền về muộn: chỉ log cảnh báo `LATE PAYMENT`, chưa có luồng hoàn tiền/đối soát.
- `/internal/products/batch` không có xác thực – production phải chặn ở mạng/gateway.
- JWT HS256 dùng secret chung; production nên dùng RS256 + JWKS.
- Outbox relay giả định 1 instance mỗi service (nhiều instance có thể lệch thứ tự theo key).
- Chưa có integration test với Testcontainers và chưa có circuit breaker cho lời gọi product-service.
