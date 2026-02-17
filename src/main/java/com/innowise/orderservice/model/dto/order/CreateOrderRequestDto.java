package com.innowise.orderservice.model.dto.order;

import com.innowise.orderservice.model.dto.orderitem.OrderItemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequestDto(
        @NotBlank(message = "userEmail is required")
        @Email(message = "userEmail must be a valid email")
        @Size(max = 255, message = "userEmail length must be less than or equal to 255")
        String userEmail,
        @NotBlank(message = "status is required")
        String status,
        @NotEmpty(message = "items must not be empty")
        List<@Valid OrderItemRequestDto> items
) {
}
