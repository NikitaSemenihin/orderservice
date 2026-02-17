package com.innowise.orderservice.mapper;

import com.innowise.orderservice.model.dto.order.CreateOrderRequestDto;
import com.innowise.orderservice.model.dto.order.OrderResponseDto;
import com.innowise.orderservice.model.dto.order.UpdateOrderRequestDto;
import com.innowise.orderservice.model.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "items", target = "orderItems")
    Order toEntity(CreateOrderRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "items", target = "orderItems")
    void updateEntity(UpdateOrderRequestDto dto, @MappingTarget Order order);

    @Mapping(source = "orderItems", target = "items")
    OrderResponseDto toResponse(Order entity);
}
