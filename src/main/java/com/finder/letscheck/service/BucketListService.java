package com.finder.letscheck.service;

import com.finder.letscheck.model.Item;
import com.finder.letscheck.model.UserBucketList;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.repository.UserBucketListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BucketListService {

    private final UserBucketListRepository bucketListRepository;
    private final ItemRepository itemRepository;

    /**
     * Add item to current user's bucket list.
     *
     * Launch-safe behavior:
     * - duplicate add should not crash app
     * - if already bookmarked, just return silently
     */
    public void addToBucketList(String userId, String itemId) {
        if (bucketListRepository.findByUserIdAndItemId(userId, itemId).isPresent()) {
            return;
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        UserBucketList entry = UserBucketList.builder()
                .userId(userId)
                .itemId(itemId)
                .itemName(item.getItemName())
                .city(item.getCity())
                .areaName(item.getAreaName())
                .createdAt(Instant.now().toString())
                .build();

        try {
            bucketListRepository.save(entry);
        } catch (DuplicateKeyException ignored) {
            // Safe for concurrent duplicate bookmark clicks
        }
    }

    /**
     * Remove item from bucket list.
     */
    public void removeFromBucketList(String userId, String itemId) {
        bucketListRepository.findByUserIdAndItemId(userId, itemId)
                .ifPresent(bucketListRepository::delete);
    }

    /**
     * Get all saved items for current user.
     */
    public List<UserBucketList> getUserBucketList(String userId) {
        return bucketListRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Check whether current user already bookmarked this item.
     */
    public boolean isBookmarked(String userId, String itemId) {
        return bucketListRepository.findByUserIdAndItemId(userId, itemId).isPresent();
    }
}