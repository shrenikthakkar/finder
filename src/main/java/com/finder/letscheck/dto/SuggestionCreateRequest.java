package com.finder.letscheck.dto;

import lombok.Data;

/**
 * Request DTO for user-submitted suggestion.
 */
@Data
public class SuggestionCreateRequest {

    private String userId;

    private String itemName;
    private String restaurantName;

    private String city;
    private String areaName;

    private String category;
    private String subCategory;

    private Double price;
    private String currency;

    private Boolean isVeg;

    private String note;

    private Double latitude;
    private Double longitude;
}