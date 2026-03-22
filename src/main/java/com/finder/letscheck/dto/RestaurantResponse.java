package com.finder.letscheck.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestaurantResponse {

    private String id;
    private String name;
    private String normalizedName;
    private String fullAddress;
    private String landmark;
    private String areaName;
    private String city;
    private String state;
    private String country;
    private String pincode;

    private Double avgRestaurantRating;
    private Integer ratingCount;
    private Integer itemCount;

    private String status;
    private Boolean isVerified;
    private Boolean isActive;
}