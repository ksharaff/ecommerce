package com.khaled.ecommerce.paymentservice.event;

import java.time.Instant;

public record PaymentResultEvent(
    Long orderId,
    boolean success,
    String reason, // why it failed if it failed
    Instant occurredAt
) {}
