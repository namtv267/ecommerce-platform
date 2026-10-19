package com.shop.product.adapter.out.persistence;

import com.shop.product.application.port.out.ProductQueryPort;
import com.shop.product.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
class ClickHouseProductAdapter implements ProductQueryPort {

    private final ProductMapper mapper;

    ClickHouseProductAdapter(ProductMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Product> findByIds(Collection<String> ids) {
        return mapper.selectByIds(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Product> findActive(int limit, int offset) {
        return mapper.selectActive(limit, offset).stream().map(this::toDomain).toList();
    }

    private Product toDomain(ProductRow r) {
        return new Product(r.getId(), r.getSku(), r.getName(), r.getDescription(),
                r.getPrice(), r.getCurrency(), r.getStock(), r.isActive());
    }
}
