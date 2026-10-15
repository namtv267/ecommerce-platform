package com.shop.contracts;

public final class Topics {
    private Topics() {}

    /** order-service -> payment-service (lệnh) */
    public static final String PAYMENT_COMMANDS = "payment.commands";
    /** payment-service -> order-service (kết quả) */
    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String DLT_SUFFIX = ".DLT";
}
