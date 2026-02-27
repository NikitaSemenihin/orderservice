package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.item.ItemRequestDto;
import com.innowise.orderservice.model.dto.item.ItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {
    ItemResponseDto createItem(ItemRequestDto request);

    ItemResponseDto getItemById(Long id);

    Page<ItemResponseDto> getItems(Pageable pageable);

    ItemResponseDto updateItemById(Long id, ItemRequestDto request);

    void deleteItemById(Long id);
}
