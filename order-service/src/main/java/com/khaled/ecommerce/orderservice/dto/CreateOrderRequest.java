package com.khaled.ecommerce.orderservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record  CreateOrderRequest(
    @NotNull Long userId,
    // Nested objects and lists need their own explicit @Valid.
    @NotEmpty @Valid List<OrderItemDto> items
) {}
