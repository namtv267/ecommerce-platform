package com.shop.payment.application.port.out;

public interface ProcessedMessagePort {
    boolean isProcessed(String messageId);

    /** @return true nếu là lần xử lý đầu tiên của messageId (gọi trong transaction nghiệp vụ). */
    boolean markProcessed(String messageId);
}
