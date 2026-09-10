package com.khaled.ecommerce.orderservice.client;

import java.math.BigDecimal;
// Deliberately narrower than product-service's own ProductResponse - Order only needs id, price,
// and stock. If Product service adds an unrelated field to its response tomorrow, Order service
// doesn't need to know or care. This is the same instinct as duplicating ErrorResponse earlier.
public record ProductClientResponse (Long id, String name, BigDecimal price, Integer stockQuantity){}
