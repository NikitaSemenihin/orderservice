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
import com.innowise.orderservice.model.dto.orderitem.OrderItemRequestDto;
import com.innowise.orderservice.model.dto.orderitem.OrderItemResponseDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderShouldSaveAndReturnCombinedResponse() {
        CreateOrderRequestDto request = new CreateOrderRequestDto(
                "test@example.com",
                OrderStatus.NEW,
                List.of(new OrderItemRequestDto(10L, 2))
        );
        Order mappedOrder = buildOrder(1L, "test@example.com", OrderStatus.NEW, false);
        mappedOrder.setOrderItems(List.of(buildOrderItem(10L, 2)));
        Order savedOrder = mappedOrder;
        OrderResponseDto orderResponse = buildOrderResponse(1L, "test@example.com", OrderStatus.NEW, BigDecimal.valueOf(20));
        UserClientDto user = buildUser("test@example.com");

        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(buildItem(10L, "Pen", BigDecimal.TEN)));
        when(orderRepository.save(mappedOrder)).thenReturn(savedOrder);
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(user);
        when(orderMapper.toResponse(savedOrder)).thenReturn(orderResponse);

        OrderWithUserResponseDto result = orderService.createOrder(request);

        assertThat(result.order()).isEqualTo(orderResponse);
        assertThat(result.user()).isEqualTo(user);
        assertThat(mappedOrder.getTotalPrice()).isEqualByComparingTo("20");
    }

    @Test
    void createOrderShouldThrowWhenItemMissing() {
        CreateOrderRequestDto request = new CreateOrderRequestDto(
                "test@example.com",
                OrderStatus.NEW,
                List.of(new OrderItemRequestDto(10L, 2))
        );
        Order mappedOrder = buildOrder(1L, "test@example.com", OrderStatus.NEW, false);
        mappedOrder.setOrderItems(List.of(buildOrderItem(10L, 2)));

        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderByIdShouldReturnCombinedResponseWhenFound() {
        Order order = buildOrder(2L, "found@example.com", OrderStatus.PROCESSING, false);
        OrderResponseDto orderResponse = buildOrderResponse(2L, "found@example.com", OrderStatus.PROCESSING, BigDecimal.ONE);
        UserClientDto user = buildUser("found@example.com");

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(userServiceClient.getUserByEmail("found@example.com")).thenReturn(user);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderWithUserResponseDto result = orderService.getOrderById(2L);

        assertThat(result.order()).isEqualTo(orderResponse);
        assertThat(result.user()).isEqualTo(user);
    }

    @Test
    void getOrderByIdShouldThrowWhenOrderMissing() {
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(100L));
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void getOrdersShouldThrowForInvalidDateRange() {
        Pageable pageable = PageRequest.of(0, 10);
        Instant from = Instant.parse("2024-01-02T00:00:00Z");
        Instant to = Instant.parse("2024-01-01T00:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrders(null, null, from, to, pageable));
        verifyNoInteractions(orderRepository, userServiceClient);
    }

    @Test
    void getOrdersShouldReturnEmptyPageWithoutCallingUserServiceWhenNoOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<OrderWithUserResponseDto> result = orderService.getOrders(null, null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void getOrdersShouldReturnCombinedPageForResolvedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = buildOrder(3L, "page@example.com", OrderStatus.COMPLETED, false);
        Page<Order> orders = new PageImpl<>(List.of(order), pageable, 1);
        OrderResponseDto dto = buildOrderResponse(3L, "page@example.com", OrderStatus.COMPLETED, BigDecimal.valueOf(42));
        UserClientDto user = buildUser("page@example.com");

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orders);
        when(orderMapper.toResponse(order)).thenReturn(dto);
        when(userServiceClient.getUsersByEmails(anySet())).thenReturn(Map.of("page@example.com", user));

        Page<OrderWithUserResponseDto> result = orderService.getOrders(List.of(OrderStatus.COMPLETED), null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).user()).isEqualTo(user);

        ArgumentCaptor<java.util.Set<String>> emailsCaptor = ArgumentCaptor.forClass(java.util.Set.class);
        verify(userServiceClient).getUsersByEmails(emailsCaptor.capture());
        assertThat(emailsCaptor.getValue()).containsExactly("page@example.com");
    }

    @Test
    void getOrdersShouldThrowWhenBatchUserResponseMissesEmail() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = buildOrder(4L, "missing@example.com", OrderStatus.NEW, false);
        Page<Order> orders = new PageImpl<>(List.of(order), pageable, 1);
        OrderResponseDto dto = buildOrderResponse(4L, "missing@example.com", OrderStatus.NEW, BigDecimal.ONE);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orders);
        when(orderMapper.toResponse(order)).thenReturn(dto);
        when(userServiceClient.getUsersByEmails(anySet())).thenReturn(Map.of());

        assertThrows(RemoteUserNotFoundException.class,
                () -> orderService.getOrders(null, null, null, null, pageable));
    }

    @Test
    void updateOrderByIdShouldMergeItemsRecalculateAndReturnResponse() {
        UpdateOrderRequestDto request = new UpdateOrderRequestDto(
                OrderStatus.PROCESSING,
                List.of(new OrderItemRequestDto(20L, 3))
        );

        Order existing = buildOrder(5L, "update@example.com", OrderStatus.NEW, false);
        existing.setOrderItems(new java.util.ArrayList<>(List.of(buildOrderItem(10L, 1))));
        OrderResponseDto response = buildOrderResponse(5L, "update@example.com", OrderStatus.PROCESSING, BigDecimal.valueOf(30));
        UserClientDto user = buildUser("update@example.com");

        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(itemRepository.findById(20L)).thenReturn(Optional.of(buildItem(20L, "Notebook", BigDecimal.TEN)));
        when(orderRepository.save(existing)).thenReturn(existing);
        when(userServiceClient.getUserByEmail("update@example.com")).thenReturn(user);
        when(orderMapper.toResponse(existing)).thenReturn(response);

        OrderWithUserResponseDto result = orderService.updateOrderById(5L, request);

        assertThat(result.order()).isEqualTo(response);
        assertThat(result.user()).isEqualTo(user);
        assertThat(existing.getOrderItems()).hasSize(1);
        assertThat(existing.getOrderItems().get(0).getItem().getId()).isEqualTo(20L);
        assertThat(existing.getTotalPrice()).isEqualByComparingTo("30");
        verify(orderMapper).updateEntity(request, existing);
    }

    @Test
    void updateOrderByIdShouldThrowWhenOrderMissing() {
        UpdateOrderRequestDto request = new UpdateOrderRequestDto(
                OrderStatus.PROCESSING,
                List.of(new OrderItemRequestDto(20L, 3))
        );
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrderById(999L, request));
        verify(orderMapper, never()).updateEntity(any(), any());
    }

    @Test
    void deleteOrderByIdShouldDeleteWhenOrderExists() {
        Order order = buildOrder(7L, "delete@example.com", OrderStatus.CANCELED, false);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        orderService.deleteOrderById(7L);

        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrderByIdShouldThrowWhenMissing() {
        when(orderRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrderById(8L));
        verify(orderRepository, never()).delete(any(Order.class));
    }

    private static Item buildItem(Long id, String name, BigDecimal price) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    private static OrderItem buildOrderItem(Long itemId, int qty) {
        OrderItem orderItem = new OrderItem();
        Item item = new Item();
        item.setId(itemId);
        orderItem.setItem(item);
        orderItem.setQuantity(qty);
        return orderItem;
    }

    private static Order buildOrder(Long id, String email, OrderStatus status, boolean deleted) {
        Order order = new Order();
        order.setId(id);
        order.setUserEmail(email);
        order.setStatus(status);
        order.setDeleted(deleted);
        order.setTotalPrice(BigDecimal.ZERO);
        return order;
    }

    private static OrderResponseDto buildOrderResponse(Long id, String email, OrderStatus status, BigDecimal total) {
        return new OrderResponseDto(
                id,
                email,
                status,
                total,
                false,
                Instant.now(),
                Instant.now(),
                List.of(new OrderItemResponseDto(1L, 1L, "item", BigDecimal.ONE, 1))
        );
    }

    private static UserClientDto buildUser(String email) {
        return new UserClientDto(
                1L,
                "John",
                "Doe",
                email,
                true,
                LocalDate.parse("2000-01-01"),
                Instant.now(),
                Instant.now()
        );
    }
}
