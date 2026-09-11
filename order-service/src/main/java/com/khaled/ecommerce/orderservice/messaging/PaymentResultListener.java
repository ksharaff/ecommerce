package com.khaled.ecommerce.orderservice.messaging;

import com.khaled.ecommerce.orderservice.event.PaymentResultEvent;
import com.khaled.ecommerce.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    public static final String PAYMENT_RESULTS_TOPIC = "payment-results";

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final OrderService orderService;

    public PaymentResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = PAYMENT_RESULTS_TOPIC)
    public void onPaymentResult(PaymentResultEvent event) {
        log.info("Received payment result for order {}: successful={}", event.orderId(), event.successful());
        orderService.applyPaymentResult(event.orderId(), event.successful());
    }
}