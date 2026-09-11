package com.khaled.ecommerce.productservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Product only cares about items[] here - but the record must still match the full published
// shape, since the JSON carries every field. Extra fields you ignore are harmless; missing ones
// you expected are what break.
public record OrderPlacedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        Instant occurredAt
) {
    public record OrderItemEvent(Long productId, Integer quantity) {}
}