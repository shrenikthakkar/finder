package com.finder.letscheck.service;

import com.finder.letscheck.dto.RestaurantRequest;
import com.finder.letscheck.dto.RestaurantResponse;
import com.finder.letscheck.model.Location;
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

        String normalizedName = normalizeText(request.getName());
        String normalizedCity = normalizeText(request.getCity());
        String normalizedAreaName = normalizeText(request.getAreaName());
        Location location = new Location(
                "Point",
                new double[]{request.getLongitude(), request.getLatitude()}
        );

        restaurantRepository.findByNormalizedNameAndNormalizedCityAndNormalizedAreaName(
                normalizedName,
                normalizedCity,
                normalizedAreaName
        ).ifPresent(existing -> {
            throw new RuntimeException("Restaurant already exists with same name in this area and city");
        });

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName().trim())
                .normalizedName(normalizedName)
                .fullAddress(request.getFullAddress().trim())
                .landmark(request.getLandmark() != null ? request.getLandmark().trim() : null)
                .areaName(request.getAreaName().trim())
                .normalizedAreaName(normalizedAreaName)
                .city(request.getCity().trim())
                .normalizedCity(normalizedCity)
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
                .location(location)
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
                .normalizedAreaName(restaurant.getNormalizedAreaName())
                .city(restaurant.getCity())
                .normalizedCity(restaurant.getNormalizedCity())
                .state(restaurant.getState())
                .country(restaurant.getCountry())
                .pincode(restaurant.getPincode())
                .location(restaurant.getLocation())
                .avgRestaurantRating(restaurant.getAvgRestaurantRating())
                .ratingCount(restaurant.getRatingCount())
                .itemCount(restaurant.getItemCount())
                .status(restaurant.getStatus())
                .isVerified(restaurant.getIsVerified())
                .isActive(restaurant.getIsActive())
                .build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}