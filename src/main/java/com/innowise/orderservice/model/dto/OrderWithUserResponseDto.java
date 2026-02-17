package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.dto.order.OrderResponseDto;

public record OrderWithUserResponseDto(
        OrderResponseDto order,
        UserClientDto user
) {
}
