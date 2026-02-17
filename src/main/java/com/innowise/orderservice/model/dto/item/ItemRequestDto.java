package com.innowise.orderservice.model.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemRequestDto(
        @NotBlank(message = "name is required")
        String name,
        @Positive(message = "price must be greater than 0")
        BigDecimal price
) {
}
