package com.shop.payment.config;

import com.shop.payment.application.port.out.PaymentGatewayPort;
import com.shop.payment.application.port.out.PaymentRepositoryPort;
import com.shop.payment.application.port.out.PaymentResultPublisherPort;
import com.shop.payment.application.port.out.ProcessedMessagePort;
import com.shop.payment.application.port.out.TransactionPort;
import com.shop.payment.application.service.ProcessPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

    @Bean
    ProcessPaymentService processPaymentService(PaymentRepositoryPort payments, PaymentGatewayPort gateway,
                                                PaymentResultPublisherPort publisher,
                                                ProcessedMessagePort processedMessages, TransactionPort tx) {
        return new ProcessPaymentService(payments, gateway, publisher, processedMessages, tx);
    }
}
