package com.khaled.ecommerce.productservice.messaging;

import com.khaled.ecommerce.productservice.event.OrderPlacedEvent;
import com.khaled.ecommerce.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final ProductService productService;

    public OrderEventListener(ProductService productService) {
        this.productService = productService;
    }

    @KafkaListener(topics = ORDER_EVENTS_TOPIC)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order-placed event for order {} - decrementing stock", event.orderId());

        productService.decrementStockForOrder(
                event.orderId(),
                event.items().stream()
                        .map(item -> new ProductService.StockDecrement(item.productId(), item.quantity()))
                        .toList());
    }
}