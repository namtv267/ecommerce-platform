package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.BatchRequest;
import com.shop.product.adapter.in.web.dto.ProductResponse;
import com.shop.product.application.port.in.GetProductsByIdsUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API nội bộ service-to-service (order-service gọi để lấy giá/tồn kho).
 * KHÔNG có xác thực: ở môi trường thật chỉ cho truy cập trong mạng nội bộ / chặn ở API gateway.
 */
@RestController
@RequestMapping("/internal/products")
class InternalProductController {

    private final GetProductsByIdsUseCase getProductsByIds;

    InternalProductController(GetProductsByIdsUseCase getProductsByIds) {
        this.getProductsByIds = getProductsByIds;
    }

    @PostMapping("/batch")
    List<ProductResponse> batch(@Valid @RequestBody BatchRequest request) {
        return getProductsByIds.getByIds(request.ids()).stream().map(ProductResponse::from).toList();
    }
}
