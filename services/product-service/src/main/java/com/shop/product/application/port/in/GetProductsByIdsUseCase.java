package com.shop.product.application.port.in;

import com.shop.product.domain.model.Product;

import java.util.Collection;
import java.util.List;

/** Dùng bởi order-service: id không tồn tại sẽ không có trong kết quả. */
public interface GetProductsByIdsUseCase {
    List<Product> getByIds(Collection<String> ids);
}
