-- Chạy 1 lần khi volume Postgres còn trống.
-- Mỗi service sở hữu database riêng (database-per-service) trên cùng 1 instance cho môi trường local.
CREATE DATABASE auth_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
