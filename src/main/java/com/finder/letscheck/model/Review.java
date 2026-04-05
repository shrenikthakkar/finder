package com.finder.letscheck.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reviews")
@CompoundIndex(
        name = "uniq_user_target_review",
        def = "{'userId': 1, 'targetType': 1, 'targetId': 1}",
        unique = true
)
public class Review {

    @Id
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