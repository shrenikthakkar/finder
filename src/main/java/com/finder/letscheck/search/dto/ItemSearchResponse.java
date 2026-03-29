package com.finder.letscheck.search.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemSearchResponse {

    private String itemId;
    private String itemName;
    private String restaurantId;
    private String restaurantName;

    private String city;
    private String areaName;

    private Double avgItemRating;
    private Integer ratingCount;

    private Double distanceInKm;

    /**
     * Whether current user has already bookmarked this item.
     * Null/false for guest users.
     */
    private Boolean isBookmarked;
}