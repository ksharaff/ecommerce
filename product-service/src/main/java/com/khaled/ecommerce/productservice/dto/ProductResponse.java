package com.khaled.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.khaled.ecommerce.productservice.model.Product;

public record ProductResponse (
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    String category,
    Instant createdAt,
    Instant updatedAt
) {
    
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.getId(), 
            product.getName(), 
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getCategory(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
