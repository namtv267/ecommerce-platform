package com.shop.contracts;

public final class MsgHeaders {
    private MsgHeaders() {}

    /** UUID của dòng outbox; consumer dùng để khử trùng lặp. */
    public static final String MESSAGE_ID = "message-id";
    public static final String MESSAGE_TYPE = "message-type";
}
