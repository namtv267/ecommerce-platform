package com.shop.order.application;

import com.shop.order.application.port.out.TransactionPort;

import java.util.function.Supplier;

/** Transaction giả cho unit test: chạy thẳng. */
class DirectTransaction implements TransactionPort {
    @Override
    public <T> T execute(Supplier<T> work) {
        return work.get();
    }
}
