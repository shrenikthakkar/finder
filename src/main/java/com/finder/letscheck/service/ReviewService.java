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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final int MAX_COMMENT_LENGTH = 300;
    private static final int MAX_COMMENT_WORDS = 20;

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
            throw new RuntimeException("You have already reviewed this item");
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

        try {
            Review savedReview = reviewRepository.save(review);

            // Best-effort aggregate updates: do not fail the user action if
            // inconsistent legacy data exists (for example missing restaurant).
            try {
                updateItemAggregates(item, request.getRating());
            } catch (Exception e) {
                log.error("Failed to update item aggregates for itemId={}", item.getId(), e);
            }

            try {
                updateRestaurantAggregates(item.getRestaurantId(), request.getRating());
            } catch (Exception e) {
                log.error("Failed to update restaurant aggregates for restaurantId={}", item.getRestaurantId(), e);
            }

            return mapToResponse(savedReview);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate review blocked for userId={} targetId={}", request.getUserId(), item.getId(), e);
            throw new RuntimeException("You have already reviewed this item");
        }
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
        if (restaurantId == null || restaurantId.isBlank()) {
            log.warn("Skipping restaurant aggregate update because restaurantId is blank");
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant == null) {
            log.warn("Skipping restaurant aggregate update because restaurant was not found. restaurantId={}", restaurantId);
            return;
        }

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

        review.setRating(newRating);
        review.setComment(normalizeAndValidateComment(newComment));
        review.setIsEdited(true);
        review.setUpdatedAt(Instant.now().toString());

        Review savedReview = reviewRepository.save(review);

        try {
            updateItemAggregateOnUpdate(review.getTargetId(), oldRating, newRating);
        } catch (Exception e) {
            log.error("Failed to update item aggregate on review update. itemId={}", review.getTargetId(), e);
        }

        try {
            updateRestaurantAggregateOnUpdate(review.getRestaurantId(), oldRating, newRating);
        } catch (Exception e) {
            log.error("Failed to update restaurant aggregate on review update. restaurantId={}", review.getRestaurantId(), e);
        }

        return mapToResponse(savedReview);
    }

    public void deleteReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new RuntimeException("Review already inactive");
        }

        review.setStatus("INACTIVE");
        review.setUpdatedAt(Instant.now().toString());
        reviewRepository.save(review);

        int rating = review.getRating();

        try {
            updateItemAggregateOnDelete(review.getTargetId(), rating);
        } catch (Exception e) {
            log.error("Failed to update item aggregate on review delete. itemId={}", review.getTargetId(), e);
        }

        try {
            updateRestaurantAggregateOnDelete(review.getRestaurantId(), rating);
        } catch (Exception e) {
            log.error("Failed to update restaurant aggregate on review delete. restaurantId={}", review.getRestaurantId(), e);
        }
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
        if (restaurantId == null || restaurantId.isBlank()) {
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant == null) {
            log.warn("Skipping restaurant aggregate delete update because restaurant was not found. restaurantId={}", restaurantId);
            return;
        }

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
        item.setAvgItemRating(count == 0 ? 0 : (double) sum / count);

        itemRepository.save(item);
    }

    private void updateRestaurantAggregateOnUpdate(String restaurantId, int oldRating, int newRating) {
        if (restaurantId == null || restaurantId.isBlank()) {
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant == null) {
            log.warn("Skipping restaurant aggregate update because restaurant was not found. restaurantId={}", restaurantId);
            return;
        }

        int sum = restaurant.getRatingSum() - oldRating + newRating;
        int count = restaurant.getRatingCount();

        restaurant.setRatingSum(sum);
        restaurant.setAvgRestaurantRating(count == 0 ? 0 : (double) sum / count);

        restaurantRepository.save(restaurant);
    }

    private String normalizeAndValidateComment(String comment) {
        if (comment == null) {
            return null;
        }

        String normalized = comment.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new RuntimeException("Review comment is too long");
        }

        int wordCount = normalized.split("\\s+").length;
        if (wordCount > MAX_COMMENT_WORDS) {
            throw new RuntimeException("Review comment can have at most 20 words");
        }

        return normalized;
    }
}