package com.finder.letscheck.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    private String name;

    @NotBlank(message = "Normalized name is required")
    private String normalizedName;

    @NotBlank(message = "Full address is required")
    private String fullAddress;

    private String landmark;

    @NotBlank(message = "Area name is required")
    private String areaName;

    @NotBlank(message = "City is required")
    private String city;

    private String state;
    private String country;
    private String pincode;

    private String createdBy;
    private String createdByType;
}