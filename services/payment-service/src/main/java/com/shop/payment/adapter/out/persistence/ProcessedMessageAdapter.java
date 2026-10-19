package com.shop.payment.adapter.out.persistence;

import com.shop.messaging.ProcessedMessageStore;
import com.shop.payment.application.port.out.ProcessedMessagePort;
import org.springframework.stereotype.Component;

@Component
class ProcessedMessageAdapter implements ProcessedMessagePort {

    private final ProcessedMessageStore store;

    ProcessedMessageAdapter(ProcessedMessageStore store) {
        this.store = store;
    }

    @Override
    public boolean isProcessed(String messageId) {
        return store.isProcessed(messageId);
    }

    @Override
    public boolean markProcessed(String messageId) {
        return store.markProcessed(messageId);
    }
}
