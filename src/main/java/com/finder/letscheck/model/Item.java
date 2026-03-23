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
@Document(collection = "items")
public class Item {

    @Id
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
    private Integer ratingSum;

    private String areaName;
    private String normalizedAreaName;

    private String city;
    private String normalizedCity;

    private Location location;

    private String createdBy;
    private String createdByType;

    private String status;
    private Boolean isVerified;
    private Boolean isActive;
}