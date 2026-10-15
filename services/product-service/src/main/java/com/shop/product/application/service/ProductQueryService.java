package com.shop.product.application.service;

import com.shop.product.application.port.in.GetProductUseCase;
import com.shop.product.application.port.in.GetProductsByIdsUseCase;
import com.shop.product.application.port.in.ListProductsUseCase;
import com.shop.product.application.port.out.ProductQueryPort;
import com.shop.product.domain.exception.ProductNotFoundException;
import com.shop.product.domain.model.Product;

import java.util.Collection;
import java.util.List;

public class ProductQueryService implements GetProductUseCase, ListProductsUseCase, GetProductsByIdsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductQueryPort products;

    public ProductQueryService(ProductQueryPort products) {
        this.products = products;
    }

    @Override
    public Product get(String id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public List<Product> listActive(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return products.findActive(safeSize, safePage * safeSize);
    }

    @Override
    public List<Product> getByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of(); // tránh sinh câu SQL "IN ()" không hợp lệ
        }
        return products.findByIds(ids);
    }
}
