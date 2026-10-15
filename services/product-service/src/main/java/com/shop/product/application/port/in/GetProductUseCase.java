package com.shop.product.application.port.in;

import com.shop.product.domain.model.Product;

public interface GetProductUseCase {
    Product get(String id);
}
