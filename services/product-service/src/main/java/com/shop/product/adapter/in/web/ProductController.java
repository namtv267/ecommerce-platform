package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ProductResponse;
import com.shop.product.application.port.in.GetProductUseCase;
import com.shop.product.application.port.in.ListProductsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API công khai cho storefront (không cần đăng nhập). */
@RestController
@RequestMapping("/api/products")
class ProductController {

    private final GetProductUseCase getProduct;
    private final ListProductsUseCase listProducts;

    ProductController(GetProductUseCase getProduct, ListProductsUseCase listProducts) {
        this.getProduct = getProduct;
        this.listProducts = listProducts;
    }

    @GetMapping({"", "/"})
    List<ProductResponse> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return listProducts.listActive(page, size).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable String id) {
        return ProductResponse.from(getProduct.get(id));
    }
}
