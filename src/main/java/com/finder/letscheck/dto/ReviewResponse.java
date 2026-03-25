package com.finder.letscheck.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponse {

    private String id;

    private String userId;
    private String userName;

    private String targetType;
    private String targetId;
    private String targetName;

    private String restaurantId;
    private String restaurantName;

    private Integer rating;
    private String comment;

    private String status;
    private Boolean isEdited;

    private String createdAt;
    private String updatedAt;
}