package com.innowise.orderservice.model.dto.order;

import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
        Long id,
        Long userId,
        String status,
        double totalPrice,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponseDto> items
) {
}
