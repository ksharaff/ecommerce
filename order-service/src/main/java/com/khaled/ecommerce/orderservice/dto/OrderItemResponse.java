package com.khaled.ecommerce.orderservice.dto;

import java.math.BigDecimal;

import com.khaled.ecommerce.orderservice.model.OrderItem;

public record OrderItemResponse(Long productId, Integer quantity, BigDecimal unitPriceAtOrderTime) {
    public static OrderItemResponse fromEntity(OrderItem orderItem) {
        return new OrderItemResponse(
            orderItem.getProductId(),
            orderItem.getQuantity(),
            orderItem.getUnitPriceAtOrderTime()
        );
    }
}
