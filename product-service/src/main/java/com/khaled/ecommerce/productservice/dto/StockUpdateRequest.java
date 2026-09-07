package com.khaled.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record StockUpdateRequest (
    @NotBlank @PositiveOrZero Integer quantity
) {}
