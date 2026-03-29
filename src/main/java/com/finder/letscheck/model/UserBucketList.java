package com.finder.letscheck.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Stores user saved / bucket list items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_bucket_list")
@CompoundIndex(name = "user_item_idx", def = "{'userId': 1, 'itemId': 1}", unique = true)
public class UserBucketList {

    @Id
    private String id;

    private String userId;

    private String itemId;

    private String itemName;

    private String normalizedItemName;

    private String city;
    private String normalizedCity;

    private String areaName;
    private String normalizedAreaName;

    private String createdAt;

    private Boolean isActive;
}