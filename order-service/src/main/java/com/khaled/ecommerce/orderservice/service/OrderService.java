package com.khaled.ecommerce.orderservice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.khaled.ecommerce.orderservice.client.ProductClientResponse;
import com.khaled.ecommerce.orderservice.client.ProductServiceClient;
import com.khaled.ecommerce.orderservice.client.UserServiceClient;
import com.khaled.ecommerce.orderservice.messaging.OrderEventPublisher;
import com.khaled.ecommerce.orderservice.model.Order;
import com.khaled.ecommerce.orderservice.model.OrderItem;
import com.khaled.ecommerce.orderservice.model.OrderStatus;
import com.khaled.ecommerce.orderservice.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderEventPublisher eventPublisher; // add alongside the three existing fields



    public OrderService(OrderRepository orderRepository,
                        ProductServiceClient productServiceClient,
                        UserServiceClient userServiceClient,
                        OrderEventPublisher eventPublisher) { // fourth parameter
        this.orderRepository = orderRepository;
        this.productServiceClient = productServiceClient;
        this.userServiceClient = userServiceClient;
        this.eventPublisher = eventPublisher;
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
        Order saved = orderRepository.save(order);
        // Publish AFTER the save succeeds, never before. If the save fails, no event goes out -
        // otherwise Payment would happily charge for an order that doesn't exist.
        //
        // Worth naming honestly: this still isn't bulletproof. If the app crashes in the
        // microsecond between the save committing and the send happening, the order exists
        // with no event ever published. The real fix is the "transactional outbox" pattern -
        // writing the event into the same database transaction as the order, then relaying it
        // separately. That's genuinely the correct production answer, and genuinely more
        // complexity than this project needs right now. Knowing the gap exists (and being able
        // to say so in an interview) is worth more than pretending it doesn't.
        eventPublisher.publishOrderPlaced(saved);
        return saved;

    }

    @Transactional
    public void applyPaymentResult(Long orderId, boolean successful) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Idempotency again, Order's version: only act if the order is still waiting on a decision.
        // A duplicate payment-result event then changes nothing instead of re-transitioning an
        // order that's already settled.
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} already in status {} - ignoring duplicate payment result", orderId, order.getStatus());
            return;
        }

        order.setStatus(successful ? OrderStatus.CONFIRMED : OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} moved to {}", orderId, order.getStatus());
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> listOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
