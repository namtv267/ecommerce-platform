package com.shop.product.application;

import com.shop.product.application.port.out.ProductQueryPort;
import com.shop.product.application.service.ProductQueryService;
import com.shop.product.domain.exception.ProductNotFoundException;
import com.shop.product.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock ProductQueryPort port;

    @Test
    void getByIds_emptyInput_doesNotHitStorage() {
        ProductQueryService service = new ProductQueryService(port);

        assertThat(service.getByIds(List.of())).isEmpty();
        verifyNoInteractions(port);
    }

    @Test
    void get_unknownProduct_throwsNotFound() {
        when(port.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ProductQueryService(port).get("x"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listActive_clampsPageSize() {
        new ProductQueryService(port).listActive(-5, 10_000);

        verify(port).findActive(100, 0);
    }

    @Test
    void canFulfill_requiresActiveAndEnoughStock() {
        Product p = new Product("p", "sku", "n", "d", new BigDecimal("1.00"), "USD", 3, true);

        assertThat(p.canFulfill(3)).isTrue();
        assertThat(p.canFulfill(4)).isFalse();
        assertThat(new Product("p", "sku", "n", "d", new BigDecimal("1.00"), "USD", 3, false).canFulfill(1)).isFalse();
    }
}
