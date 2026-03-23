package com.finder.letscheck.controller;

import com.finder.letscheck.dto.ItemRequest;
import com.finder.letscheck.dto.ItemResponse;
import com.finder.letscheck.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ItemResponse addItem(@Valid @RequestBody ItemRequest request) {
        return itemService.addItem(request);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<ItemResponse> getItemsByRestaurant(@PathVariable String restaurantId) {
        return itemService.getItemsByRestaurant(restaurantId);
    }

    @GetMapping("/{id}")
    public ItemResponse getItemById(@PathVariable String id) {
        return itemService.getItemById(id);
    }

    @GetMapping
    public List<ItemResponse> getAllItems() {
        return itemService.getAllItems();
    }
}