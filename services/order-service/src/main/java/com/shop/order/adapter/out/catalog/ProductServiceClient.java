package com.shop.order.adapter.out.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shop.order.application.port.out.ProductCatalogPort;
import com.shop.order.domain.exception.ProductCatalogUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Gọi đồng bộ product-service để lấy giá/tồn kho. Có timeout; lỗi -> 503 cho client (client có thể retry với cùng Idempotency-Key). */
@Component
class ProductServiceClient implements ProductCatalogPort {

    record BatchRequest(List<String> ids) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductDto(String id, String name, BigDecimal price, String currency, int stock, boolean active) {}

    private final RestClient client;

    ProductServiceClient(RestClient.Builder builder, @Value("${app.product-service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public List<CatalogProduct> findByIds(Collection<String> ids) {
        try {
            ProductDto[] body = client.post()
                    .uri("/internal/products/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new BatchRequest(List.copyOf(ids)))
                    .retrieve()
                    .body(ProductDto[].class);
            if (body == null) {
                return List.of();
            }
            return Arrays.stream(body)
                    .map(p -> new CatalogProduct(p.id(), p.name(), p.price(), p.currency(), p.stock(), p.active()))
                    .toList();
        } catch (RestClientException e) {
            throw new ProductCatalogUnavailableException(e);
        }
    }
}
