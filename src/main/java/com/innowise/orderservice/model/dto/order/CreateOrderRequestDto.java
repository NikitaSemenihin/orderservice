package com.innowise.orderservice.model.dto.order;

import com.innowise.orderservice.model.dto.orderitem.OrderItemRequestDto;
import com.innowise.orderservice.model.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequestDto(
        @NotNull(message = "status is required")
        OrderStatus status,
        @NotEmpty(message = "items must not be empty")
        List<@Valid OrderItemRequestDto> items
) {
}
