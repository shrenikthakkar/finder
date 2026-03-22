package com.finder.letscheck.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "restaurants")
public class Restaurant {

    @Id
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
    private Integer ratingSum;
    private Integer itemCount;

    private String createdBy;
    private String createdByType;
    private String status;

    private Boolean isVerified;
    private Boolean isActive;
}