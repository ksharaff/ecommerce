package com.khaled.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemDto(
    @NotNull Long productId,
    @NotNull @Positive Integer quantity
) {}
