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
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    private String userId;
    private String userName;

    private String targetType;   // ITEM or RESTAURANT
    private String targetId;
    private String targetName;

    private String restaurantId;
    private String restaurantName;

    private Integer rating;
    private String comment;

    private String status;       // ACTIVE / INACTIVE
    private Boolean isEdited;

    private String createdAt;
    private String updatedAt;
}