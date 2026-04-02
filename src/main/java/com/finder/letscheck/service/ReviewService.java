package com.finder.letscheck.service;

import com.finder.letscheck.dto.ReviewRequest;
import com.finder.letscheck.dto.ReviewResponse;
import com.finder.letscheck.model.Item;
import com.finder.letscheck.model.Restaurant;
import com.finder.letscheck.model.Review;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.repository.RestaurantRepository;
import com.finder.letscheck.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ItemRepository itemRepository;
    private final RestaurantRepository restaurantRepository;

    public ReviewResponse addItemReview(ReviewRequest request) {

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + request.getItemId()));

        reviewRepository.findByUserIdAndTargetTypeAndTargetId(
                request.getUserId().trim(),
                "ITEM",
                item.getId()
        ).ifPresent(existing -> {
            throw new RuntimeException("User has already reviewed this item");
        });

        String now = Instant.now().toString();

        Review review = Review.builder()
                .userId(request.getUserId().trim())
                .userName(request.getUserName().trim())
                .targetType("ITEM")
                .targetId(item.getId())
                .targetName(item.getItemName())
                .restaurantId(item.getRestaurantId())
                .restaurantName(item.getRestaurantName())
                .rating(request.getRating())
                .comment(normalizeAndValidateComment(request.getComment()))
                .status("ACTIVE")
                .isEdited(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Review savedReview = reviewRepository.save(review);

        updateItemAggregates(item, request.getRating());
        updateRestaurantAggregates(item.getRestaurantId(), request.getRating());

        return mapToResponse(savedReview);
    }

    public List<ReviewResponse> getReviewsByItem(String itemId) {
        return reviewRepository.findByTargetTypeAndTargetId("ITEM", itemId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsByUser(String userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void updateItemAggregates(Item item, Integer rating) {
        int currentCount = item.getRatingCount() == null ? 0 : item.getRatingCount();
        int currentSum = item.getRatingSum() == null ? 0 : item.getRatingSum();

        int newCount = currentCount + 1;
        int newSum = currentSum + rating;
        double newAverage = (double) newSum / newCount;

        item.setRatingCount(newCount);
        item.setRatingSum(newSum);
        item.setAvgItemRating(newAverage);

        itemRepository.save(item);
    }

    private void updateRestaurantAggregates(String restaurantId, Integer rating) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + restaurantId));

        int currentCount = restaurant.getRatingCount() == null ? 0 : restaurant.getRatingCount();
        int currentSum = restaurant.getRatingSum() == null ? 0 : restaurant.getRatingSum();

        int newCount = currentCount + 1;
        int newSum = currentSum + rating;
        double newAverage = (double) newSum / newCount;

        restaurant.setRatingCount(newCount);
        restaurant.setRatingSum(newSum);
        restaurant.setAvgRestaurantRating(newAverage);

        restaurantRepository.save(restaurant);
    }

    /**
     * Validates and normalizes review comment text.
     *
     * Rules for launch:
     * - comment is optional
     * - trim extra spaces
     * - allow short, meaningful reviews only
     * - reject overly lengthy text to keep launch-stage data compact
     */
    private String normalizeAndValidateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        String normalized = comment.trim().replaceAll("\\s+", " ");

        int wordCount = normalized.split(" ").length;
        if (wordCount > 20) {
            throw new RuntimeException("Review comment can have at most 20 words");
        }

        return normalized;
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .targetType(review.getTargetType())
                .targetId(review.getTargetId())
                .targetName(review.getTargetName())
                .restaurantId(review.getRestaurantId())
                .restaurantName(review.getRestaurantName())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .isEdited(review.getIsEdited())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    public ReviewResponse updateReview(String reviewId, Integer newRating, String newComment) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new RuntimeException("Cannot update inactive review");
        }

        int oldRating = review.getRating();

        // update review
        review.setRating(newRating);
        review.setComment(normalizeAndValidateComment(newComment));
        review.setIsEdited(true);
        review.setUpdatedAt(java.time.Instant.now().toString());

        Review savedReview = reviewRepository.save(review);

        // adjust aggregates
        updateItemAggregateOnUpdate(review.getTargetId(), oldRating, newRating);
        updateRestaurantAggregateOnUpdate(review.getRestaurantId(), oldRating, newRating);

        return mapToResponse(savedReview);
    }

    public void deleteReview(String reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new RuntimeException("Review already inactive");
        }

        review.setStatus("INACTIVE");
        review.setUpdatedAt(java.time.Instant.now().toString());

        reviewRepository.save(review);

        int rating = review.getRating();

        updateItemAggregateOnDelete(review.getTargetId(), rating);
        updateRestaurantAggregateOnDelete(review.getRestaurantId(), rating);
    }

    private void updateItemAggregateOnDelete(String itemId, int rating) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        int sum = item.getRatingSum() - rating;
        int count = item.getRatingCount() - 1;

        item.setRatingSum(sum);
        item.setRatingCount(count);
        item.setAvgItemRating(count == 0 ? 0 : (double) sum / count);

        itemRepository.save(item);
    }

    private void updateRestaurantAggregateOnDelete(String restaurantId, int rating) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        int sum = restaurant.getRatingSum() - rating;
        int count = restaurant.getRatingCount() - 1;

        restaurant.setRatingSum(sum);
        restaurant.setRatingCount(count);
        restaurant.setAvgRestaurantRating(count == 0 ? 0 : (double) sum / count);

        restaurantRepository.save(restaurant);
    }

    private void updateItemAggregateOnUpdate(String itemId, int oldRating, int newRating) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        int sum = item.getRatingSum() - oldRating + newRating;
        int count = item.getRatingCount();

        item.setRatingSum(sum);
        item.setAvgItemRating((double) sum / count);

        itemRepository.save(item);
    }

    private void updateRestaurantAggregateOnUpdate(String restaurantId, int oldRating, int newRating) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        int sum = restaurant.getRatingSum() - oldRating + newRating;
        int count = restaurant.getRatingCount();

        restaurant.setRatingSum(sum);
        restaurant.setAvgRestaurantRating((double) sum / count);

        restaurantRepository.save(restaurant);
    }
}