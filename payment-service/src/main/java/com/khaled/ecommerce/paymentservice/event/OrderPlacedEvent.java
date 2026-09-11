package com.khaled.ecommerce.paymentservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
    Long orderId,
    Long userId,
    BigDecimal totalAmount,
    List<OrderItemEvent> items,
    Instant occurredAt
) {
    public record OrderItemEvent(
        Long productId,
        Integer quantity
    ) {
    }
}
