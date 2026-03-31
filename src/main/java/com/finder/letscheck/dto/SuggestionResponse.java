package com.finder.letscheck.dto;

import com.finder.letscheck.model.enums.SuggestionStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for suggestion data.
 */
@Data
@Builder
public class SuggestionResponse {

    private String id;

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

    private SuggestionStatus status;

    private String linkedItemId;

    private Integer rewardPointsGranted;

    private String reviewReason;

    private String createdAt;
    private String reviewedAt;

    // Optional coordinates provided with the suggestion
    private Double latitude;
    private Double longitude;
}