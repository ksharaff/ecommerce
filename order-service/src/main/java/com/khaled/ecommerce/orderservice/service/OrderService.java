package com.khaled.ecommerce.orderservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.khaled.ecommerce.orderservice.client.ProductClientResponse;
import com.khaled.ecommerce.orderservice.client.ProductServiceClient;
import com.khaled.ecommerce.orderservice.client.UserServiceClient;
import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.model.OrderItem;
import com.khaled.ecommerce.orderservice.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;

    public OrderService(OrderRepository orderRepository, ProductServiceClient productServiceClient, UserServiceClient userServiceClient) {
        this.orderRepository = orderRepository;
        this.productServiceClient = productServiceClient;
        this.userServiceClient = userServiceClient;
    }

    public Order placeOrder(Long userId, List<OrderItemRequest> requestedItems) {
        // Confirm the user is real before doing anything else. UserNotFoundException or
        // ServiceUnavailableException propagate straight up - the controller (next step)
        // translates each into the right HTTP status.
        userServiceClient.getUser(userId);

        Order order = new Order(userId);

        for(OrderItemRequest requested : requestedItems) {
            ProductClientResponse product = productServiceClient.getProduct(requested.productId());

            if(product.stockQuantity() < requested.quantity()) {
                throw new InsufficientStockException(requested.productId(), product.stockQuantity(), requested.quantity());
            }

            // The price snapshot happens HERE, at the exact moment of ordering - using addItem(),
            // which we deliberately made the only way to attach an item, back when we designed Order.
            OrderItem item = new OrderItem(product.id(), requested.quantity(), product.price());
            order.addItem(item);
        }
        return orderRepository.save(order);
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> listOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
