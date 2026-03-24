package com.innowise.orderservice.service;

import com.innowise.orderservice.event.PaymentCreatedEvent;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.model.entity.PaymentStatus;
import com.innowise.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentEventService paymentEventService;

    @Test
    void shouldCompleteOrderForSuccessfulPayment() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "payment-1",
                10L,
                20L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-20T10:00:00Z"),
                BigDecimal.valueOf(99.90)
        );
        Order order = buildOrder(10L, OrderStatus.NEW);

        when(orderRepository.findByPaymentId("payment-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        paymentEventService.handlePaymentCreated(event);

        assertThat(order.getPaymentId()).isEqualTo("payment-1");
        assertThat(order.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void shouldCancelOrderForFailedPayment() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "payment-2",
                11L,
                21L,
                PaymentStatus.FAILED,
                Instant.parse("2026-03-20T10:00:00Z"),
                BigDecimal.valueOf(55.00)
        );
        Order order = buildOrder(11L, OrderStatus.PROCESSING);

        when(orderRepository.findByPaymentId("payment-2")).thenReturn(Optional.empty());
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));

        paymentEventService.handlePaymentCreated(event);

        assertThat(order.getPaymentId()).isEqualTo("payment-2");
        assertThat(order.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void shouldIgnoreDuplicatePaymentEvent() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "payment-3",
                12L,
                22L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-20T10:00:00Z"),
                BigDecimal.TEN
        );

        when(orderRepository.findByPaymentId("payment-3")).thenReturn(Optional.of(buildOrder(12L, OrderStatus.COMPLETED)));

        paymentEventService.handlePaymentCreated(event);

        verify(orderRepository, never()).findById(12L);
    }

    @Test
    void shouldThrowWhenOrderDoesNotExist() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "payment-4",
                99L,
                22L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-20T10:00:00Z"),
                BigDecimal.TEN
        );

        when(orderRepository.findByPaymentId("payment-4")).thenReturn(Optional.empty());
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentEventService.handlePaymentCreated(event))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    private Order buildOrder(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(1L);
        order.setUserEmail("user@example.com");
        order.setStatus(status);
        order.setTotalPrice(BigDecimal.ONE);
        order.setDeleted(false);
        return order;
    }
}
