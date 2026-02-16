package com.innowise.orderservice.model.dto.order;

public record OrderItemResponseDto(
        Long id,
        Long itemId,
        String itemName,
        double itemPrice,
        Integer quantity
) {
}
