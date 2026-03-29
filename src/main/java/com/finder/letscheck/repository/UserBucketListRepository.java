package com.finder.letscheck.repository;

import com.finder.letscheck.model.UserBucketList;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserBucketListRepository extends MongoRepository<UserBucketList, String> {

    Optional<UserBucketList> findByUserIdAndItemId(String userId, String itemId);

    Optional<UserBucketList> findByUserIdAndItemIdAndIsActiveTrue(String userId, String itemId);

    List<UserBucketList> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Fetch all active bookmarked entries for a given user and a list of item IDs.
     * Used to enrich search results with bookmark status in one DB call.
     */
    List<UserBucketList> findByUserIdAndItemIdInAndIsActiveTrue(String userId, List<String> itemIds);
}