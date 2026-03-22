package com.finder.letscheck.controller;

import com.finder.letscheck.dto.RestaurantRequest;
import com.finder.letscheck.dto.RestaurantResponse;
import com.finder.letscheck.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public RestaurantResponse addRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return restaurantService.saveRestaurant(request);
    }

    @GetMapping
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    @GetMapping("/{id}")
    public RestaurantResponse getRestaurantById(@PathVariable String id) {
        return restaurantService.getRestaurantById(id);
    }
}