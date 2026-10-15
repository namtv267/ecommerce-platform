package com.shop.product.config;

import com.shop.product.application.port.out.ProductQueryPort;
import com.shop.product.application.service.ProductQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

    @Bean
    ProductQueryService productQueryService(ProductQueryPort products) {
        return new ProductQueryService(products);
    }
}
