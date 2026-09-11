package com.khaled.ecommerce.paymentservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.khaled.ecommerce.paymentservice.event.OrderPlacedEvent;
import com.khaled.ecommerce.paymentservice.event.PaymentResultEvent;
import com.khaled.ecommerce.paymentservice.model.Payment;
import com.khaled.ecommerce.paymentservice.model.PaymentStatus;
import com.khaled.ecommerce.paymentservice.repository.PaymentRepository;


@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // Deterministic simulation rule: anything over this amount is "declined".
    // Deliberately not random - you can trigger the failure path on demand by placing a big order.
    private static final BigDecimal DECLINE_THRESHOLD = new BigDecimal("1000.00");

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResultEvent processPayment(OrderPlacedEvent event) {
        // IDEMPOTENCY CHECK - the most important few lines in this whole phase.
        // Kafka guarantees "at least once" delivery, not "exactly once". A network hiccup, a consumer
        // restart, or a rebalance can all cause the same event to be delivered twice. Without this,
        // the same order gets charged twice. WITH it, a duplicate delivery is a harmless no-op that
        // replays the original decision.
        Optional<Payment> existing = paymentRepository.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            log.info("Duplicate event for order {} - already processed as {}", event.orderId(), payment.getStatus());
            return new PaymentResultEvent(
                    payment.getOrderId(),
                    payment.getStatus() == PaymentStatus.SUCCESS,
                    payment.getStatus() == PaymentStatus.SUCCESS ? null : "Payment previously declined",
                    Instant.now());
        }

        Payment payment = new Payment(event.orderId(), event.totalAmount());

        boolean approved = event.totalAmount().compareTo(DECLINE_THRESHOLD) <= 0;
        // compareTo, not equals() or < - BigDecimal.equals() considers 10.0 and 10.00 DIFFERENT
        // (it compares scale too). compareTo is almost always what you actually want.

        payment.setStatus(approved ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        paymentRepository.save(payment);

        log.info("Processed payment for order {}: {}", event.orderId(), payment.getStatus());

        return new PaymentResultEvent(
                event.orderId(),
                approved,
                approved ? null : "Amount exceeds authorization limit",
                Instant.now());
    }
}