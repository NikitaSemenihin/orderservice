package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.dto.OrderWithUserResponseDto;
import com.innowise.orderservice.model.dto.UserClientDto;
import com.innowise.orderservice.model.dto.order.CreateOrderRequestDto;
import com.innowise.orderservice.model.dto.order.OrderResponseDto;
import com.innowise.orderservice.model.dto.order.UpdateOrderRequestDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderWithUserResponseDto createOrder(CreateOrderRequestDto request) {
        Order order = orderMapper.toEntity(request);
        order.setDeleted(false);
        attachResolvedItemsAndCalculateTotal(order);
        Order savedOrder = orderRepository.save(order);
        UserClientDto user = userServiceClient.getUserByEmail(order.getUserEmail());
        return buildResponse(user, orderMapper.toResponse(savedOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderWithUserResponseDto getOrderById(Long id) {
        Order order = getExistingOrder(id);
        UserClientDto user = userServiceClient.getUserByEmail(order.getUserEmail());
        return buildResponse(user, orderMapper.toResponse(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderWithUserResponseDto> getOrders(
            List<String> statuses,
            String userEmail,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom must be before or equal to createdTo");
        }

        Specification<Order> specification = Specification
                .where(OrderSpecification.hasStatuses(statuses))
                .and(OrderSpecification.hasUserEmail(userEmail))
                .and(OrderSpecification.createdAtBetween(createdFrom, createdTo));


        return orderRepository.findAll(specification, pageable)
                .map(orderMapper::toResponse)
                .map(orderDto -> {
                    UserClientDto user = userServiceClient.getUserByEmail(orderDto.userEmail());
                    return buildResponse(user, orderDto);
                });
    }

    @Override
    @Transactional
    public OrderWithUserResponseDto updateOrderById(Long id, UpdateOrderRequestDto request) {
        Order order = getExistingOrder(id);
        orderMapper.updateEntity(request, order);
        attachResolvedItemsAndCalculateTotal(order);

        Order savedOrder = orderRepository.save(order);
        UserClientDto user = userServiceClient.getUserByEmail(order.getUserEmail());
        return buildResponse(user, orderMapper.toResponse(savedOrder));
    }

    @Override
    @Transactional
    public void deleteOrderById(Long id) {
        Order order = getExistingOrder(id);
        orderRepository.delete(order);
    }

    private Order getExistingOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    private void attachResolvedItemsAndCalculateTotal(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            order.setTotalPrice(BigDecimal.ZERO);
            return;
        }
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItem orderItem : order.getOrderItems()) {
            Long itemId = orderItem.getItem() != null ? orderItem.getItem().getId() : null;
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Item not found with id: " + itemId
                    ));

            orderItem.setOrder(order);
            orderItem.setItem(item);
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalPrice(totalPrice);
    }

    private OrderWithUserResponseDto buildResponse(UserClientDto userDto, OrderResponseDto orderDto) {
        return new OrderWithUserResponseDto(orderDto, userDto);
    }
}
