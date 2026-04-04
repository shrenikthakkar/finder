package com.finder.letscheck.controller;

import com.finder.letscheck.model.UserBucketList;
import com.finder.letscheck.security.CurrentUserService;
import com.finder.letscheck.service.BucketListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bucket-list APIs for authenticated users.
 *
 * Security decision:
 * - frontend should not send userId for bookmark operations
 * - current user is derived from JWT authentication
 */
@RestController
@RequestMapping("/bucket-list")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;
    private final CurrentUserService currentUserService;

    /**
     * Add one item to current user's bucket list.
     */
    @PostMapping
    public void add(@RequestParam String itemId) {
        bucketListService.addToBucketList(currentUserService.getCurrentUserId(), itemId);
    }

    /**
     * Remove one item from current user's bucket list.
     */
    @DeleteMapping
    public void remove(@RequestParam String itemId) {
        bucketListService.removeFromBucketList(currentUserService.getCurrentUserId(), itemId);
    }

    /**
     * Get current user's bucket list.
     */
    @GetMapping("/me")
    public List<UserBucketList> getMine() {
        return bucketListService.getUserBucketList(currentUserService.getCurrentUserId());
    }

    /**
     * Check whether current user bookmarked a given item.
     */
    @GetMapping("/check")
    public boolean check(@RequestParam String itemId) {
        return bucketListService.isBookmarked(currentUserService.getCurrentUserId(), itemId);
    }
}