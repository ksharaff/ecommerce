package com.khaled.ecommerce.orderservice.service;

// Internal input shape for OrderService - NOT the HTTP-facing DTO (that comes next step, with
// @NotNull/@Positive validation on it). This one stays plain; validation belongs at the API boundary.
public record OrderItemRequest(Long productId, Integer quantity) {}
