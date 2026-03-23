package com.finder.letscheck.dto;

import com.finder.letscheck.model.Location;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemResponse {

    private String id;

    private String restaurantId;
    private String restaurantName;

    private String itemName;
    private String normalizedItemName;

    private String category;
    private String subCategory;

    private Double price;
    private String currency;

    private Boolean isVeg;
    private Boolean isAvailable;

    private Double avgItemRating;
    private Integer ratingCount;

    private String areaName;
    private String city;

    private Location location;

    private Boolean isVerified;
    private Boolean isActive;
}