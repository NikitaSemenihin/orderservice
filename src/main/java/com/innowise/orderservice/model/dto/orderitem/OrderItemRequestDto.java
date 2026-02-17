package com.innowise.orderservice.model.dto.orderitem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto(
        @NotNull(message = "itemId is required")
        @Positive(message = "itemId must be greater than 0")
        Long itemId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be greater than 0")
        Integer quantity
) {
}
