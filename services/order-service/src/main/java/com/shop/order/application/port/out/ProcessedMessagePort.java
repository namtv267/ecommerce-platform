package com.shop.order.application.port.out;

public interface ProcessedMessagePort {
    /** @return true nếu là lần xử lý đầu tiên của messageId (gọi trong transaction nghiệp vụ). */
    boolean markProcessed(String messageId);
}
