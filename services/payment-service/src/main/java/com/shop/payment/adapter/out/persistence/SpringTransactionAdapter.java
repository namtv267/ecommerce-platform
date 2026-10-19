package com.shop.payment.adapter.out.persistence;

import com.shop.payment.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
class SpringTransactionAdapter implements TransactionPort {

    private final TransactionTemplate template;

    SpringTransactionAdapter(PlatformTransactionManager txManager) {
        this.template = new TransactionTemplate(txManager);
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
