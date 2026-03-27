package com.finder.letscheck.controller;

import com.finder.letscheck.dto.ReviewRequest;
import com.finder.letscheck.dto.ReviewResponse;
import com.finder.letscheck.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/item")
    public ReviewResponse addItemReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.addItemReview(request);
    }

    @GetMapping("/item/{itemId}")
    public List<ReviewResponse> getReviewsByItem(@PathVariable String itemId) {
        return reviewService.getReviewsByItem(itemId);
    }

    @GetMapping("/user/{userId}")
    public List<ReviewResponse> getReviewsByUser(@PathVariable String userId) {
        return reviewService.getReviewsByUser(userId);
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(
            @PathVariable String reviewId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment
    ) {
        return reviewService.updateReview(reviewId, rating, comment);
    }

    @DeleteMapping("/{reviewId}")
    public String deleteReview(@PathVariable String reviewId) {
        reviewService.deleteReview(reviewId);
        return "Review deleted successfully";
    }
}