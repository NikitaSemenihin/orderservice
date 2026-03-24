package com.innowise.orderservice.event;

import com.innowise.orderservice.service.PaymentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCreatedEventListener {
    private final PaymentEventService paymentEventService;

    @KafkaListener(topics = "${app.kafka.payment-created-topic}")
    public void handle(PaymentCreatedEvent event) {
        paymentEventService.handlePaymentCreated(event);
    }
}
