package com.finder.letscheck.controller;

import com.finder.letscheck.dto.ReviewRequest;
import com.finder.letscheck.dto.ReviewResponse;
import com.finder.letscheck.security.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    @PostMapping("/item")
    public ReviewResponse addItemReview(@Valid @RequestBody ReviewRequest request) {
        var currentUser = currentUserService.getCurrentUser();
        request.setUserId(currentUser.getId());
        request.setUserName(resolvePublicUsername(currentUser));
        return reviewService.addItemReview(request);
    }

    @GetMapping("/item/{itemId}")
    public List<ReviewResponse> getReviewsByItem(@PathVariable String itemId) {
        return reviewService.getReviewsByItem(itemId);
    }

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

    private String resolvePublicUsername(com.finder.letscheck.model.User user) {
        if (user.getPublicUsername() != null && !user.getPublicUsername().isBlank()) {
            return user.getPublicUsername().trim();
        }

        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim().toLowerCase().replaceAll("[^a-z0-9._]", "_");
        }

        return "spotzy_user";
    }
}