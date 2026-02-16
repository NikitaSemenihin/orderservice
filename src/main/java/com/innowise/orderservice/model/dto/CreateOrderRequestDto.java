package com.innowise.orderservice.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequestDto(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be greater than 0")
        Long userId,
        @NotBlank(message = "status is required")
        String status,
        @NotEmpty(message = "items must not be empty")
        List<@Valid OrderItemRequestDto> items
) {
}
