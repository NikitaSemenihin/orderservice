package com.innowise.orderservice.model.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequestDto(
        @NotBlank(message = "name is required")
        @Size(min = 1, max = 255, message = "name length must be greater than 0 and less than or equal to 255")
        String name,
        @Positive(message = "price must be greater than 0")
        @NotNull
        BigDecimal price
) {
}
