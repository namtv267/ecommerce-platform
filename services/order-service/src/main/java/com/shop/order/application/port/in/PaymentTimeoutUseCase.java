package com.shop.order.application.port.in;

public interface PaymentTimeoutUseCase {
    /** Xử lý các đơn PENDING quá hạn: gửi lại lệnh thanh toán hoặc huỷ nếu hết lượt. @return số đơn đã xử lý. */
    int handleStalePayments();
}
