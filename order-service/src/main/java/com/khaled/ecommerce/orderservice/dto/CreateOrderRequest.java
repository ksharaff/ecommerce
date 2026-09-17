package com.khaled.ecommerce.orderservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

// userId deliberately absent. It comes from the verified JWT, never from the request body -
// otherwise any caller could order as any user just by changing a number.
public record CreateOrderRequest(
        @NotEmpty
        @Valid
        List<OrderItemDto> items
) {}