package com.finder.letscheck.service;

import com.finder.letscheck.dto.ItemRequest;
import com.finder.letscheck.dto.ItemResponse;
import com.finder.letscheck.model.Item;
import com.finder.letscheck.model.Location;
import com.finder.letscheck.model.Restaurant;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final RestaurantRepository restaurantRepository;

    public ItemResponse addItem(ItemRequest request) {

        Restaurant restaurant;

        if (request.getRestaurantId() != null && !request.getRestaurantId().isBlank()) {
            restaurant = restaurantRepository.findById(request.getRestaurantId())
                    .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + request.getRestaurantId()));
        } else {
            validateRestaurantFieldsForNewRestaurant(request);

            String normalizedRestaurantName = normalizeText(request.getRestaurantName());
            String normalizedCity = normalizeText(request.getCity());
            String normalizedAreaName = normalizeText(request.getAreaName());

            restaurant = restaurantRepository.findByNormalizedNameAndNormalizedCityAndNormalizedAreaName(
                    normalizedRestaurantName,
                    normalizedCity,
                    normalizedAreaName
            ).orElseGet(() -> {
                Location restaurantLocation = new Location(
                        "Point",
                        new double[]{request.getLongitude(), request.getLatitude()}
                );

                Restaurant newRestaurant = Restaurant.builder()
                        .name(request.getRestaurantName().trim())
                        .normalizedName(normalizedRestaurantName)
                        .fullAddress(request.getFullAddress() != null ? request.getFullAddress().trim() : null)
                        .landmark(request.getLandmark() != null ? request.getLandmark().trim() : null)
                        .areaName(request.getAreaName().trim())
                        .normalizedAreaName(normalizedAreaName)
                        .city(request.getCity().trim())
                        .normalizedCity(normalizedCity)
                        .state(request.getState())
                        .country(request.getCountry())
                        .pincode(request.getPincode())
                        .location(restaurantLocation)
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

                return restaurantRepository.save(newRestaurant);
            });
        }

        String normalizedItemName = normalizeText(request.getItemName());

        itemRepository.findByRestaurantIdAndNormalizedItemName(
                restaurant.getId(),
                normalizedItemName
        ).ifPresent(existingItem -> {
            throw new RuntimeException("Item already exists in this restaurant");
        });

        Location itemLocation = new Location(
                "Point",
                new double[]{request.getLongitude(), request.getLatitude()}
        );

        Item item = Item.builder()
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .itemName(request.getItemName().trim())
                .normalizedItemName(normalizedItemName)
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .price(request.getPrice())
                .currency("INR")
                .isVeg(request.getIsVeg())
                .isAvailable(true)
                .avgItemRating(0.0)
                .ratingCount(0)
                .ratingSum(0)
                .areaName(restaurant.getAreaName())
                .normalizedAreaName(restaurant.getNormalizedAreaName())
                .city(restaurant.getCity())
                .normalizedCity(restaurant.getNormalizedCity())
                .location(itemLocation)
                .createdBy(request.getCreatedBy())
                .createdByType(request.getCreatedByType())
                .status("APPROVED")
                .isVerified(false)
                .isActive(true)
                .build();

        Item savedItem = itemRepository.save(item);

        restaurant.setItemCount(restaurant.getItemCount() == null ? 1 : restaurant.getItemCount() + 1);
        restaurantRepository.save(restaurant);

        return mapToResponse(savedItem);
    }

    public List<ItemResponse> getItemsByRestaurant(String restaurantId) {
        return itemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ItemResponse getItemById(String id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + id));
        return mapToResponse(item);
    }

    private ItemResponse mapToResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .restaurantId(item.getRestaurantId())
                .restaurantName(item.getRestaurantName())
                .itemName(item.getItemName())
                .normalizedItemName(item.getNormalizedItemName())
                .category(item.getCategory())
                .subCategory(item.getSubCategory())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .isVeg(item.getIsVeg())
                .isAvailable(item.getIsAvailable())
                .avgItemRating(item.getAvgItemRating())
                .ratingCount(item.getRatingCount())
                .areaName(item.getAreaName())
                .city(item.getCity())
                .location(item.getLocation())
                .isVerified(item.getIsVerified())
                .isActive(item.getIsActive())
                .build();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void validateRestaurantFieldsForNewRestaurant(ItemRequest request) {
        if (request.getRestaurantName() == null || request.getRestaurantName().isBlank()) {
            throw new RuntimeException("Restaurant name is required when restaurantId is not provided");
        }
        if (request.getAreaName() == null || request.getAreaName().isBlank()) {
            throw new RuntimeException("Area name is required when creating a new restaurant");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            throw new RuntimeException("City is required when creating a new restaurant");
        }
    }

    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}