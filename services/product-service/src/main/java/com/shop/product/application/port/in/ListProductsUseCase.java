package com.shop.product.application.port.in;

import com.shop.product.domain.model.Product;

import java.util.List;

public interface ListProductsUseCase {
    List<Product> listActive(int page, int size);
}
