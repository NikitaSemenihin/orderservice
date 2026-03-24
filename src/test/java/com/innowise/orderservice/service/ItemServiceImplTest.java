package com.innowise.orderservice.service;

import com.innowise.orderservice.exception.ItemInUseException;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.model.dto.item.ItemRequestDto;
import com.innowise.orderservice.model.dto.item.ItemResponseDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderItemRepository;
import com.innowise.orderservice.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void createItemShouldSaveAndReturnMappedResponse() {
        ItemRequestDto request = new ItemRequestDto("Keyboard", BigDecimal.valueOf(99.99));
        Item item = buildItem(1L, "Keyboard", BigDecimal.valueOf(99.99));
        ItemResponseDto response = buildItemResponse(item);

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponse(item)).thenReturn(response);

        ItemResponseDto result = itemService.createItem(request);

        assertThat(result).isEqualTo(response);
        verify(itemMapper).toEntity(request);
        verify(itemRepository).save(item);
        verify(itemMapper).toResponse(item);
    }

    @Test
    void getItemByIdShouldReturnMappedResponseWhenItemExists() {
        Item item = buildItem(1L, "Mouse", BigDecimal.valueOf(49.50));
        ItemResponseDto response = buildItemResponse(item);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(item)).thenReturn(response);

        ItemResponseDto result = itemService.getItemById(1L);

        assertThat(result).isEqualTo(response);
        verify(itemRepository).findById(1L);
        verify(itemMapper).toResponse(item);
    }

    @Test
    void getItemByIdShouldThrowWhenItemMissing() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.getItemById(10L));

        verify(itemRepository).findById(10L);
        verifyNoInteractions(itemMapper);
    }

    @Test
    void getItemsShouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Item item = buildItem(1L, "Laptop", BigDecimal.valueOf(1200));
        ItemResponseDto response = buildItemResponse(item);
        Page<Item> itemPage = new PageImpl<>(List.of(item), pageable, 1);

        when(itemRepository.findAll(pageable)).thenReturn(itemPage);
        when(itemMapper.toResponse(item)).thenReturn(response);

        Page<ItemResponseDto> result = itemService.getItems(pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(itemRepository).findAll(pageable);
        verify(itemMapper).toResponse(item);
    }

    @Test
    void updateItemByIdShouldUpdateAndReturnMappedResponse() {
        ItemRequestDto request = new ItemRequestDto("Monitor", BigDecimal.valueOf(250));
        Item existing = buildItem(2L, "Old monitor", BigDecimal.valueOf(100));
        ItemResponseDto response = buildItemResponse(existing);

        when(itemRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(existing);
        when(itemMapper.toResponse(existing)).thenReturn(response);

        ItemResponseDto result = itemService.updateItemById(2L, request);

        assertThat(result).isEqualTo(response);
        verify(itemMapper).updateEntity(request, existing);
        verify(itemRepository).save(existing);
        verify(itemMapper).toResponse(existing);
    }

    @Test
    void updateItemByIdShouldThrowWhenItemMissing() {
        ItemRequestDto request = new ItemRequestDto("Monitor", BigDecimal.valueOf(250));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.updateItemById(99L, request));

        verify(itemRepository).findById(99L);
        verify(itemMapper, never()).updateEntity(any(), any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void deleteItemByIdShouldThrowWhenItemInUse() {
        when(orderItemRepository.existsByItemId(5L)).thenReturn(true);

        assertThrows(ItemInUseException.class, () -> itemService.deleteItemById(5L));

        verify(orderItemRepository).existsByItemId(5L);
        verify(itemRepository, never()).findById(any());
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void deleteItemByIdShouldDeleteWhenItemExistsAndNotInUse() {
        Item item = buildItem(5L, "SSD", BigDecimal.valueOf(150));

        when(orderItemRepository.existsByItemId(5L)).thenReturn(false);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));

        itemService.deleteItemById(5L);

        verify(orderItemRepository).existsByItemId(5L);
        verify(itemRepository).findById(5L);
        verify(itemRepository).delete(item);
    }

    @Test
    void deleteItemByIdShouldThrowWhenItemMissing() {
        when(orderItemRepository.existsByItemId(6L)).thenReturn(false);
        when(itemRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.deleteItemById(6L));

        verify(orderItemRepository).existsByItemId(6L);
        verify(itemRepository).findById(6L);
        verify(itemRepository, never()).delete(any());
    }

    private static Item buildItem(Long id, String name, BigDecimal price) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        return item;
    }

    private static ItemResponseDto buildItemResponse(Item item) {
        return new ItemResponseDto(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
