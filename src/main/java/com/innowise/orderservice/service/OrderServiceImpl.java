package com.innowise.orderservice.service;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.exception.RemoteUserNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.dto.OrderWithUserResponseDto;
import com.innowise.orderservice.model.dto.UserClientDto;
import com.innowise.orderservice.model.dto.order.CreateOrderRequestDto;
import com.innowise.orderservice.model.dto.order.OrderResponseDto;
import com.innowise.orderservice.model.dto.order.UpdateOrderRequestDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            List<OrderStatus> statuses,
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

        Page<Order> ordersPage = orderRepository.findAll(specification, pageable);
        List<OrderResponseDto> orderDtos = ordersPage
                .stream()
                .map(orderMapper::toResponse)
                .toList();

        Set<String> emails = orderDtos.stream()
                .map(OrderResponseDto::userEmail)
                .collect(Collectors.toSet());
        if (emails.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, ordersPage.getTotalElements());
        }
        Map<String, UserClientDto> usersByEmail = userServiceClient.getUsersByEmails(emails);


        List<OrderWithUserResponseDto> response = orderDtos.stream()
                .map(orderDto -> buildResponse(getUserOrThrow(orderDto.userEmail(), usersByEmail), orderDto))
                .toList();

        return new PageImpl<>(response, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional
    public OrderWithUserResponseDto updateOrderById(Long id, UpdateOrderRequestDto request) {
        Order order = getExistingOrder(id);
        orderMapper.updateEntity(request, order);
        mergeOrderItems(order, request);
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
        Set<Long> seenItemIds = new HashSet<>();
        for (OrderItem orderItem : order.getOrderItems()) {
            Long itemId = orderItem.getItem() != null ? orderItem.getItem().getId() : null;
            if (!seenItemIds.add(itemId)) {
                throw new IllegalArgumentException("Duplicate itemId in order items: " + itemId);
            }
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

    private void mergeOrderItems(Order order, UpdateOrderRequestDto request) {
        Map<Long, OrderItem> existingByItemId = new HashMap<>();
        for (OrderItem existing : order.getOrderItems()) {
            Long existingItemId = existing.getItem() != null ? existing.getItem().getId() : null;
            existingByItemId.put(existingItemId, existing);
        }

        List<OrderItem> mergedItems = new ArrayList<>();
        for (var requestItem : request.items()) {
            OrderItem target = existingByItemId.get(requestItem.itemId());
            if (target == null) {
                target = new OrderItem();
            }

            Item itemRef = new Item();
            itemRef.setId(requestItem.itemId());

            target.setOrder(order);
            target.setItem(itemRef);
            target.setQuantity(requestItem.quantity());
            mergedItems.add(target);
        }

        order.getOrderItems().clear();
        order.getOrderItems().addAll(mergedItems);
    }

    private OrderWithUserResponseDto buildResponse(UserClientDto userDto, OrderResponseDto orderDto) {
        return new OrderWithUserResponseDto(orderDto, userDto);
    }

    private UserClientDto getUserOrThrow(String email, Map<String, UserClientDto> usersByEmail) {
        UserClientDto user = usersByEmail.get(email);
        if (user == null) {
            throw new RemoteUserNotFoundException("User not found in batch response for email: " + email);
        }
        return user;
    }
}
