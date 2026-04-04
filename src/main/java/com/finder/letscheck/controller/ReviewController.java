package com.finder.letscheck.controller;

import com.finder.letscheck.dto.ReviewRequest;
import com.finder.letscheck.dto.ReviewResponse;
import com.finder.letscheck.security.CurrentUserService;
import com.finder.letscheck.service.ReviewService;
import com.finder.letscheck.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Review APIs.
 *
 * Security rule:
 * - review creation should use authenticated user identity
 * - frontend should not control userId/userName for write actions
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    /**
     * Add review for current logged-in user.
     */
    @PostMapping("/item")
    public ReviewResponse addItemReview(@Valid @RequestBody ReviewRequest request) {
        var currentUser = currentUserService.getCurrentUser();
        request.setUserId(currentUser.getId());
        request.setUserName(currentUser.getName());
        return reviewService.addItemReview(request);
    }

    /**
     * Get reviews for one item.
     */
    @GetMapping("/item/{itemId}")
    public List<ReviewResponse> getReviewsByItem(@PathVariable String itemId) {
        return reviewService.getReviewsByItem(itemId);
    }

    /**
     * Get reviews for current logged-in user.
     */
    @GetMapping("/me")
    public List<ReviewResponse> getMyReviews() {
        return reviewService.getReviewsByUser(currentUserService.getCurrentUserId());
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