package com.innowise.orderservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ItemRequestDto(
        @NotBlank(message = "name is required")
        String name,
        @Positive(message = "price must be greater than 0")
        double price
) {
}
