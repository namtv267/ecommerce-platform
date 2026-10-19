package com.shop.order.application.port.out;

import java.util.function.Supplier;

public interface TransactionPort {

    <T> T execute(Supplier<T> work);

    default void run(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
