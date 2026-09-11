package com.khaled.ecommerce.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Structurally identical to payment-service's copy - same field names and types, different package.
// The JSON on the wire is what actually matters; the class is just each service's local view of it.
public record OrderPlacedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        Instant occurredAt
) {
    public record OrderItemEvent(Long productId, Integer quantity) {}
}