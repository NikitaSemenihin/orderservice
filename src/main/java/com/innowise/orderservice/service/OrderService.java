package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.CreateOrderRequestDto;
import com.innowise.orderservice.model.dto.OrderResponseDto;
import com.innowise.orderservice.model.dto.UpdateOrderRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(CreateOrderRequestDto request);

    OrderResponseDto getOrderById(Long id);

    Page<OrderResponseDto> getOrders(
            List<String> statuses,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    );

    List<OrderResponseDto> getOrdersByUserId(Long userId);

    OrderResponseDto updateOrderById(Long id, UpdateOrderRequestDto request);

    void deleteOrderById(Long id);
}
