package com.innowise.orderservice.model.dto.order;

import com.innowise.orderservice.model.dto.orderitem.OrderItemRequestDto;
import com.innowise.orderservice.model.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateOrderRequestDto(
        @NotBlank(message = "userEmail is required")
        @Email(message = "userEmail must be a valid email")
        @Size(max = 255, message = "userEmail length must be less than or equal to 255")
        String userEmail,
        @NotNull(message = "status is required")
        OrderStatus status,
        @NotEmpty(message = "items must not be empty")
        List<@Valid OrderItemRequestDto> items
) {
}
