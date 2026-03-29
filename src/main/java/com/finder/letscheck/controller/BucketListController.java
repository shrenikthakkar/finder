package com.finder.letscheck.controller;

import com.finder.letscheck.model.UserBucketList;
import com.finder.letscheck.service.BucketListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bucket-list")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;

    @PostMapping
    public void add(@RequestParam String userId,
                    @RequestParam String itemId) {
        bucketListService.addToBucketList(userId, itemId);
    }

    @DeleteMapping
    public void remove(@RequestParam String userId,
                       @RequestParam String itemId) {
        bucketListService.removeFromBucketList(userId, itemId);
    }

    @GetMapping("/{userId}")
    public List<UserBucketList> get(@PathVariable String userId) {
        return bucketListService.getUserBucketList(userId);
    }

    @GetMapping("/check")
    public boolean check(@RequestParam String userId,
                         @RequestParam String itemId) {
        return bucketListService.isBookmarked(userId, itemId);
    }
}