package com.khaled.ecommerce.orderservice.dto;

import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id, Long userId, OrderStatus status, BigDecimal totalAmount,
        List<OrderItemResponse> items, Instant createdAt, Instant updatedAt
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(), order.getUserId(), order.getStatus(), order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::fromEntity).toList(),
                order.getCreatedAt(), order.getUpdatedAt()
        );
    }
}