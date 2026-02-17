package com.innowise.orderservice.model.dto.order;

import com.innowise.orderservice.model.dto.orderitem.OrderItemResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String userEmail,
        String status,
        BigDecimal totalPrice,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponseDto> items
) {
}
