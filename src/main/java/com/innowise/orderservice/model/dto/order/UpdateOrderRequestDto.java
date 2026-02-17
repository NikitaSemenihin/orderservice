package com.innowise.orderservice.model.dto.order;

import com.innowise.orderservice.model.dto.orderitem.OrderItemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateOrderRequestDto(
        @NotBlank(message = "status is required")
        String status,
        @NotEmpty(message = "items must not be empty")
        List<@Valid OrderItemRequestDto> items
) {
}
