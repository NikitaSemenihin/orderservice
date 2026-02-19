package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.exception.ItemInUseException;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.model.dto.item.ItemRequestDto;
import com.innowise.orderservice.model.dto.item.ItemResponseDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderItemRepository;
import com.innowise.orderservice.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public ItemResponseDto createItem(ItemRequestDto request) {
        Item item = itemMapper.toEntity(request);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toResponse(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getItemById(Long id) {
        return itemMapper.toResponse(getExistingItem(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponseDto> getItems(Pageable pageable) {
        return itemRepository.findAll(pageable).map(itemMapper::toResponse);
    }

    @Override
    @Transactional
    public ItemResponseDto updateItemById(Long id, ItemRequestDto request) {
        Item item = getExistingItem(id);
        itemMapper.updateEntity(request, item);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toResponse(savedItem);
    }

    @Override
    @Transactional
    public void deleteItemById(Long id) {
        if (orderItemRepository.existsByItemId(id)) {
            throw new ItemInUseException("Cannot delete an item that is in order");
        }
        Item item = getExistingItem(id);
        itemRepository.delete(item);
    }

    private Item getExistingItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
    }
}
