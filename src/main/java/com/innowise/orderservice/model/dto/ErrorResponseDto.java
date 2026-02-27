package com.innowise.orderservice.model.dto;

import java.time.Instant;

public record ErrorResponseDto(
        Instant timestamps,
        int status,
        String error,
        String message,
        String path
) {
}
