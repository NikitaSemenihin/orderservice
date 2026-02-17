package com.innowise.orderservice.model.dto.orderitem;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        Long id,
        Long itemId,
        String itemName,
        BigDecimal itemPrice,
        Integer quantity
) {
}
