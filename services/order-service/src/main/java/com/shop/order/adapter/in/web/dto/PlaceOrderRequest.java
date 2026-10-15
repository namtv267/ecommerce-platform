package com.shop.order.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(@NotEmpty @Size(max = 50) List<@Valid ItemRequest> items) {

    public record ItemRequest(@NotBlank @Size(max = 64) String productId,
                              @Min(1) @Max(1000) int quantity) {
    }
}
