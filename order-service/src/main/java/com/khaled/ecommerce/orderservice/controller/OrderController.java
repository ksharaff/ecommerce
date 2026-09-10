package com.khaled.ecommerce.orderservice.controller;

import com.khaled.ecommerce.orderservice.dto.*;
import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.service.OrderItemRequest;
import com.khaled.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        // Map the validated, HTTP-facing DTO into the plain internal record OrderService expects -
        // keeps validation annotations out of the service layer entirely
        List<OrderItemRequest> items = request.items().stream()
                .map(dto -> new OrderItemRequest(dto.productId(), dto.quantity()))
                .toList();
        Order order = orderService.placeOrder(request.userId(), items);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.fromEntity(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.fromEntity(orderService.getOrder(id)));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestParam Long userId) {
        // No (required = false) here, unlike Product's category filter - deliberately. Without a
        // required userId, this would silently become "list every order in the system," which isn't
        // a real feature we've built (no auth/roles yet to say who's allowed to see that).
        List<Order> orders = orderService.listOrdersForUser(userId);
        return ResponseEntity.ok(orders.stream().map(OrderResponse::fromEntity).toList());
    }
}