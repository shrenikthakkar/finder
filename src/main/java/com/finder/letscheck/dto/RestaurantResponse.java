package com.finder.letscheck.dto;

import com.finder.letscheck.model.Location;
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
    private String normalizedAreaName;

    private String city;
    private String normalizedCity;

    private String state;
    private String country;
    private String pincode;

    private Location location;

    private Double avgRestaurantRating;
    private Integer ratingCount;
    private Integer itemCount;

    private String status;
    private Boolean isVerified;
    private Boolean isActive;
}