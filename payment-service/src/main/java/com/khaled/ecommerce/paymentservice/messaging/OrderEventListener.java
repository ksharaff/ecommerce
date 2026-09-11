package com.khaled.ecommerce.paymentservice.messaging;

import com.khaled.ecommerce.paymentservice.event.OrderPlacedEvent;
import com.khaled.ecommerce.paymentservice.event.PaymentResultEvent;
import com.khaled.ecommerce.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final PaymentService paymentService;
    private final PaymentEventPublisher publisher;

    public OrderEventListener(PaymentService paymentService, PaymentEventPublisher publisher) {
        this.paymentService = paymentService;
        this.publisher = publisher;
    }

    @KafkaListener(topics = ORDER_EVENTS_TOPIC)
    // That one annotation is the whole subscription. Spring runs this method automatically for
    // every message on the topic, on a background thread, for as long as the app is alive.
    // No polling loop, no manual connection handling - it's genuinely just this.
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order-placed event for order {}", event.orderId());

        // Notice this listener does almost nothing itself - it delegates straight to PaymentService.
        // Same separation-of-concerns rule as controllers: the messaging layer handles "how the
        // message arrived," the service layer handles "what to actually do about it." That's also
        // exactly what let you unit-test PaymentService with zero Kafka involved.
        PaymentResultEvent result = paymentService.processPayment(event);

        publisher.publish(result);
    }
}