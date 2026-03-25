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
                .comment(request.getComment() != null ? request.getComment().trim() : null)
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
}