package com.innowise.orderservice.event;

import com.innowise.orderservice.model.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreatedEvent(
        String id,
        Long orderId,
        Long userId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount
) {
}
