package com.finder.letscheck.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_bucket_list")
@CompoundIndex(
        name = "uniq_user_item_bookmark",
        def = "{'userId': 1, 'itemId': 1}",
        unique = true
)
public class UserBucketList {

    @Id
    private String id;

    private String userId;
    private String itemId;

    private String itemName;
    private String city;
    private String areaName;

    private String createdAt;
}