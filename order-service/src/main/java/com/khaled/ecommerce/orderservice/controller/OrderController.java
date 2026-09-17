package com.khaled.ecommerce.orderservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.khaled.ecommerce.orderservice.dto.CreateOrderRequest;
import com.khaled.ecommerce.orderservice.dto.OrderResponse;
import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.security.AuthenticatedUser;
import com.khaled.ecommerce.orderservice.service.OrderItemRequest;
import com.khaled.ecommerce.orderservice.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    private final AuthenticatedUser authenticatedUser;

    public OrderController(OrderService orderService, AuthenticatedUser authenticatedUser) {
        this.orderService = orderService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = authenticatedUser.getUserId();
        // THE actual fix. The userId now comes from a cryptographically verified token,
        // not from the request body where the caller controls it. request.userId() is
        // ignored entirely - you could delete it from the DTO.

        List<OrderItemRequest> items = request.items().stream()
                .map(dto -> new OrderItemRequest(dto.productId(), dto.quantity()))
                .toList();
        Order order = orderService.placeOrder(userId, items);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.fromEntity(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponse.fromEntity(orderService.getOrder(id)));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders() {
        // The userId comes from the verified token, not a query param - a caller can only ever
        // list their own orders. Taking it as a parameter would let anyone read someone else's
        // by guessing an id, and omitting it would silently mean "every order in the system."
        Long userId = authenticatedUser.getUserId();
        List<Order> orders = orderService.listOrdersForUser(userId);
        return ResponseEntity.ok(orders.stream().map(OrderResponse::fromEntity).toList());
    }
}