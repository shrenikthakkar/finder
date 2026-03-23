package com.finder.letscheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private String createdBy;
    private String createdByType;
}