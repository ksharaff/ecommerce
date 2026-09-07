package com.khaled.ecommerce.productservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;


    
public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @Positive BigDecimal price,
    @NotNull @PositiveOrZero Integer stockQuantity,
    String category
) {}

