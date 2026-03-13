package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.OrderWithUserResponseDto;
import com.innowise.orderservice.model.dto.order.CreateOrderRequestDto;
import com.innowise.orderservice.model.dto.order.OrderResponseDto;
import com.innowise.orderservice.model.dto.order.UpdateOrderRequestDto;
import com.innowise.orderservice.model.entity.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OrderService {
    OrderWithUserResponseDto createOrder(CreateOrderRequestDto request, HttpServletRequest httpRequest);

    OrderWithUserResponseDto getOrderById(Long id, HttpServletRequest httpRequest);

    Page<OrderWithUserResponseDto> getOrders(
            HttpServletRequest httpRequest,
            List<OrderStatus> statuses,
            String userEmail,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    );

    OrderWithUserResponseDto updateOrderById(Long id, UpdateOrderRequestDto request, HttpServletRequest httpRequest);

    void deleteOrderById(Long id, HttpServletRequest httpRequest);
}
