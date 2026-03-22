package com.finder.letscheck.service;

import com.finder.letscheck.dto.RestaurantRequest;
import com.finder.letscheck.dto.RestaurantResponse;
import com.finder.letscheck.model.Restaurant;
import com.finder.letscheck.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantResponse saveRestaurant(RestaurantRequest request) {

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .normalizedName(request.getNormalizedName())
                .fullAddress(request.getFullAddress())
                .landmark(request.getLandmark())
                .areaName(request.getAreaName())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .avgRestaurantRating(0.0)
                .ratingCount(0)
                .ratingSum(0)
                .itemCount(0)
                .createdBy(request.getCreatedBy())
                .createdByType(request.getCreatedByType())
                .status("APPROVED")
                .isVerified(false)
                .isActive(true)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return mapToResponse(saved);
    }

    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public RestaurantResponse getRestaurantById(String id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));

        return mapToResponse(restaurant);
    }

    private RestaurantResponse mapToResponse(Restaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .normalizedName(restaurant.getNormalizedName())
                .fullAddress(restaurant.getFullAddress())
                .landmark(restaurant.getLandmark())
                .areaName(restaurant.getAreaName())
                .city(restaurant.getCity())
                .state(restaurant.getState())
                .country(restaurant.getCountry())
                .pincode(restaurant.getPincode())
                .avgRestaurantRating(restaurant.getAvgRestaurantRating())
                .ratingCount(restaurant.getRatingCount())
                .itemCount(restaurant.getItemCount())
                .status(restaurant.getStatus())
                .isVerified(restaurant.getIsVerified())
                .isActive(restaurant.getIsActive())
                .build();
    }
}