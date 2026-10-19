package com.shop.order.adapter.out.persistence;

import com.shop.messaging.ProcessedMessageStore;
import com.shop.order.application.port.out.ProcessedMessagePort;
import org.springframework.stereotype.Component;

@Component
class ProcessedMessageAdapter implements ProcessedMessagePort {

    private final ProcessedMessageStore store;

    ProcessedMessageAdapter(ProcessedMessageStore store) {
        this.store = store;
    }

    @Override
    public boolean markProcessed(String messageId) {
        return store.markProcessed(messageId);
    }
}
