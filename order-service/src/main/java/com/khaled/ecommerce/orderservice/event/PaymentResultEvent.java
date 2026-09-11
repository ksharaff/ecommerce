package com.khaled.ecommerce.orderservice.event;

import java.time.Instant;

public record PaymentResultEvent(Long orderId, boolean successful, String reason, Instant occurredAt) {}