package com.shop.product.application.port.out;

import com.shop.product.domain.model.Product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductQueryPort {
    Optional<Product> findById(String id);

    List<Product> findByIds(Collection<String> ids);

    List<Product> findActive(int limit, int offset);
}
