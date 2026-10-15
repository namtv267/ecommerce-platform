CREATE DATABASE IF NOT EXISTS shop;

-- ReplacingMergeTree: "update" = insert dòng mới với updated_at lớn hơn; đọc bằng FINAL để lấy bản mới nhất.
CREATE TABLE IF NOT EXISTS shop.products
(
    id          String,
    sku         String,
    name        String,
    description String,
    price       Decimal(18, 2),
    currency    LowCardinality(String),
    stock       Int32,
    active      Bool,
    created_at  DateTime64(3, 'UTC') DEFAULT now64(3),
    updated_at  DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree(updated_at)
ORDER BY id;

INSERT INTO shop.products (id, sku, name, description, price, currency, stock, active) VALUES
 ('prod-keyboard', 'KB-001', 'Mechanical Keyboard', 'Hot-swappable 75% keyboard',      89.00, 'USD',  50, true),
 ('prod-mouse',    'MS-001', 'Wireless Mouse',      'Ergonomic 2.4GHz mouse',           29.50, 'USD', 100, true),
 ('prod-monitor',  'MN-001', '4K Monitor',          '27 inch IPS 4K display',          349.99, 'USD',  10, true),
 ('prod-hub',      'HB-001', 'USB-C Hub',           'Hết hàng - dùng để test stock',    45.00, 'USD',   0, true),
 ('prod-decline-test', 'TST-13', '[TEST] Always declined',   'Giá .13 -> mock gateway từ chối (INSUFFICIENT_FUNDS)',            10.13, 'USD', 999, true),
 ('prod-flaky-test',   'TST-14', '[TEST] Gateway timeout once', 'Giá .14 -> gateway đã trừ tiền nhưng timeout ở lần gọi đầu', 20.14, 'USD', 999, true);
