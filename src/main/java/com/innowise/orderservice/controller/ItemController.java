package com.innowise.orderservice.controller;

import com.innowise.orderservice.model.dto.item.ItemRequestDto;
import com.innowise.orderservice.model.dto.item.ItemResponseDto;
import com.innowise.orderservice.service.AccessPolicyService;
import com.innowise.orderservice.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final AccessPolicyService accessPolicyService;

    @PostMapping
    public ResponseEntity<ItemResponseDto> createItem(
            HttpServletRequest request,
            @Valid @RequestBody ItemRequestDto itemRequest
    ) {
        accessPolicyService.requireUserOrAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.createItem(itemRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getItemById(HttpServletRequest request, @PathVariable Long id) {
        accessPolicyService.requireUserOrAdmin(request);
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ItemResponseDto>> getItems(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        accessPolicyService.requireUserOrAdmin(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(itemService.getItems(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponseDto> updateItemById(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody ItemRequestDto itemRequest
    ) {
        accessPolicyService.requireAdmin(request);
        return ResponseEntity.ok(itemService.updateItemById(id, itemRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItemById(HttpServletRequest request, @PathVariable Long id) {
        accessPolicyService.requireAdmin(request);
        itemService.deleteItemById(id);
        return ResponseEntity.noContent().build();
    }
}
