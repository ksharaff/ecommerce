package com.khaled.ecommerce.paymentservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.khaled.ecommerce.paymentservice.event.PaymentResultEvent;

@Component
public class PaymentEventPublisher {

    public static final String PAYMENT_RESULTS_TOPIC = "payment-results";

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // KafkaTemplate is auto-configured by Spring from your application.properties - you never
    // construct it yourself, same as how ProductRepository appeared without an implementation.
    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentResultEvent event) {
        // The second argument is the message KEY - orderId here, deliberately.
        // Kafka guarantees ordering only WITHIN a partition, and it routes by key: every event
        // with the same key lands on the same partition. So keying by orderId means events about
        // one order always arrive in the order they were sent, even across multiple partitions.
        kafkaTemplate.send(PAYMENT_RESULTS_TOPIC, String.valueOf(event.orderId()), event);
        log.info("Published payment result for order {}: successful={}", event.orderId(), event.success());
    }
}