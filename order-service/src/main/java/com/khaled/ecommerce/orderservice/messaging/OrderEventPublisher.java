package com.khaled.ecommerce.orderservice.messaging;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.khaled.ecommerce.orderservice.event.OrderPlacedEvent;
import com.khaled.ecommerce.orderservice.model.Order;

@Component
public class OrderEventPublisher {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getItems().stream()
                        .map(item -> new OrderPlacedEvent.OrderItemEvent(item.getProductId(), item.getQuantity()))
                        .toList(),
                Instant.now());

        kafkaTemplate.send(ORDER_EVENTS_TOPIC, String.valueOf(order.getId()), event);
        log.info("Published order-placed event for order {}", order.getId());
    }
}