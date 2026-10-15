package com.shop.order.config;

import com.shop.order.application.port.out.OrderRepositoryPort;
import com.shop.order.application.port.out.PaymentRequestPort;
import com.shop.order.application.port.out.ProcessedMessagePort;
import com.shop.order.application.port.out.ProductCatalogPort;
import com.shop.order.application.port.out.TransactionPort;
import com.shop.order.application.service.OrderService;
import com.shop.order.application.service.PaymentSagaService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SagaProperties.class)
class ApplicationConfig {

    @Bean
    OrderService orderService(OrderRepositoryPort orders, ProductCatalogPort catalog,
                              PaymentRequestPort paymentRequests, TransactionPort tx) {
        return new OrderService(orders, catalog, paymentRequests, tx);
    }

    @Bean
    PaymentSagaService paymentSagaService(OrderRepositoryPort orders, PaymentRequestPort paymentRequests,
                                          ProcessedMessagePort processedMessages, TransactionPort tx,
                                          SagaProperties props) {
        return new PaymentSagaService(orders, paymentRequests, processedMessages, tx,
                props.paymentTimeout(), props.maxPaymentAttempts());
    }
}
