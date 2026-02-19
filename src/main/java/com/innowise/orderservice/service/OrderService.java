package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.OrderWithUserResponseDto;
import com.innowise.orderservice.model.dto.order.CreateOrderRequestDto;
import com.innowise.orderservice.model.dto.order.OrderResponseDto;
import com.innowise.orderservice.model.dto.order.UpdateOrderRequestDto;
import com.innowise.orderservice.model.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OrderService {
    OrderWithUserResponseDto createOrder(CreateOrderRequestDto request);

    OrderWithUserResponseDto getOrderById(Long id);

    Page<OrderWithUserResponseDto> getOrders(
            List<OrderStatus> statuses,
            String userEmail,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    );

    OrderWithUserResponseDto updateOrderById(Long id, UpdateOrderRequestDto request);

    void deleteOrderById(Long id);
}
