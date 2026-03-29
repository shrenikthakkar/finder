package com.finder.letscheck.service;

import com.finder.letscheck.model.Item;
import com.finder.letscheck.model.UserBucketList;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.repository.UserBucketListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BucketListService {

    private final UserBucketListRepository bucketListRepository;
    private final ItemRepository itemRepository;

    /**
     * Add item to user's bucket list
     */
    public void addToBucketList(String userId, String itemId) {

        // Check if already exists (active or inactive)
        Optional<UserBucketList> existing =
                bucketListRepository.findByUserIdAndItemId(userId, itemId);

        if (existing.isPresent()) {

            UserBucketList entry = existing.get();

            // If already active → do nothing
            if (Boolean.TRUE.equals(entry.getIsActive())) {
                throw new RuntimeException("Item already in bucket list");
            }

            // If inactive → reactivate
            entry.setIsActive(true);
            entry.setCreatedAt(Instant.now().toString());

            bucketListRepository.save(entry);
            return;
        }

        // Fresh insert
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        UserBucketList entry = UserBucketList.builder()
                .userId(userId)
                .itemId(itemId)
                .itemName(item.getItemName())
                .normalizedItemName(item.getNormalizedItemName())
                .city(item.getCity())
                .normalizedCity(item.getNormalizedCity())
                .areaName(item.getAreaName())
                .normalizedAreaName(item.getNormalizedAreaName())
                .createdAt(Instant.now().toString())
                .isActive(true)
                .build();

        bucketListRepository.save(entry);
    }

    /**
     * Remove item from bucket list (soft delete)
     */
    public void removeFromBucketList(String userId, String itemId) {

        UserBucketList entry = bucketListRepository
                .findByUserIdAndItemIdAndIsActiveTrue(userId, itemId)
                .orElseThrow(() -> new RuntimeException("Item not in bucket list"));

        entry.setIsActive(false);

        bucketListRepository.save(entry);
    }

    /**
     * Get all bucket list items for a user
     */
    public List<UserBucketList> getUserBucketList(String userId) {
        return bucketListRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Check if item is bookmarked
     */
    public boolean isBookmarked(String userId, String itemId) {
        return bucketListRepository
                .findByUserIdAndItemIdAndIsActiveTrue(userId, itemId)
                .isPresent();
    }
}