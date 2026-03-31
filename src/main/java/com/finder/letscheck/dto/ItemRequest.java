package com.finder.letscheck.dto;

import jakarta.validation.constraints.NotBlank;
// latitude/longitude are optional for programmatic flows (e.g. creating from suggestions)
import lombok.Data;

@Data
public class ItemRequest {

    // Existing restaurant flow
    private String restaurantId;

    // New restaurant flow
    private String restaurantName;
    private String fullAddress;
    private String landmark;
    private String areaName;
    private String city;
    private String state;
    private String country;
    private String pincode;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String category;
    private String subCategory;
    private Double price;
    private Boolean isVeg;

    // Optional latitude (may be null for suggestions)
    private Double latitude;

    // Optional longitude (may be null for suggestions)
    private Double longitude;

    private String createdBy;
    private String createdByType;
}