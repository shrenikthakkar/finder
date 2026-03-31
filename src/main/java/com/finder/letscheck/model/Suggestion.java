package com.finder.letscheck.model;

import com.finder.letscheck.model.enums.SuggestionStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Stores user-submitted food suggestions.
 *
 * Important:
 * - does NOT directly create the real item
 * - first goes through moderation
 * - reward is granted only after approval
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "suggestions")
@CompoundIndex(
        name = "user_item_area_pending_idx",
        def = "{'userId': 1, 'normalizedItemName': 1, 'normalizedAreaName': 1, 'normalizedCity': 1}"
)
public class Suggestion {

    @Id
    private String id;

    /**
     * User who submitted this suggestion.
     */
    private String userId;

    /**
     * Suggested food item details.
     */
    private String itemName;
    private String normalizedItemName;

    private String restaurantName;
    private String normalizedRestaurantName;

    private String city;
    private String normalizedCity;

    private String areaName;
    private String normalizedAreaName;

    private String category;
    private String subCategory;

    private Double price;
    private String currency;

    private Boolean isVeg;

    /**
     * Optional note from the user.
     * Example:
     * "Very famous breakfast item here"
     */
    private String note;

    /**
     * Moderation status of the suggestion.
     */
    private SuggestionStatus status;

    /**
     * If approved and turned into a new item, or merged into existing item,
     * this links to the final item.
     */
    private String linkedItemId;

    /**
     * Points granted after approval.
     * 0 until moderation happens.
     */
    private Integer rewardPointsGranted;

    /**
     * Review metadata.
     */
    private String reviewReason;
    private String reviewedBy;
    private String reviewedAt;

    /**
     * Audit timestamps.
     */
    private String createdAt;
    private String updatedAt;

    private Double latitude;
    private Double longitude;
}