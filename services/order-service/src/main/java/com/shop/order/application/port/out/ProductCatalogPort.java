package com.shop.order.application.port.out;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface ProductCatalogPort {

    /** Id không tồn tại sẽ không có trong kết quả. */
    List<CatalogProduct> findByIds(Collection<String> ids);

    record CatalogProduct(String id, String name, BigDecimal price, String currency, int stock, boolean active) {}
}
