package com.innowise.orderservice.service;

import com.innowise.orderservice.event.PaymentCreatedEvent;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.model.entity.PaymentStatus;
import com.innowise.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentEventService {
    private final OrderRepository orderRepository;

    @Transactional
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        Order alreadyProcessedOrder = orderRepository.findByPaymentId(event.id()).orElse(null);
        if (alreadyProcessedOrder != null) {
            return;
        }

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + event.orderId()));

        if (order.getPaymentId() != null || isTerminal(order)) {
            return;
        }

        order.setPaymentId(event.id());
        order.setPaymentStatus(event.status().name());

        if (PaymentStatus.SUCCESS.equals(event.status())) {
            order.setStatus(OrderStatus.COMPLETED);
        } else if (PaymentStatus.FAILED.equals(event.status())) {
            order.setStatus(OrderStatus.CANCELED);
        }
    }

    private boolean isTerminal(Order order) {
        return order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELED;
    }
}
