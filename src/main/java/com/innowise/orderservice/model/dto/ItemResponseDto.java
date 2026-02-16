package com.innowise.orderservice.model.dto;

import java.time.Instant;

public record ItemResponseDto(
        Long id,
        String name,
        double price,
        Instant createdAt,
        Instant updatedAt
) {
}
