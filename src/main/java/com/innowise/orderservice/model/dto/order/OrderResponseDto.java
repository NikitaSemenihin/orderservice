package com.innowise.orderservice.model.dto.order;

import com.innowise.orderservice.model.dto.orderitem.OrderItemResponseDto;
import com.innowise.orderservice.model.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String userEmail,
        OrderStatus status,
        BigDecimal totalPrice,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponseDto> items
) {
}
