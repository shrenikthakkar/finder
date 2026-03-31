package com.finder.letscheck.service;

import com.finder.letscheck.dto.SuggestionCreateRequest;
import com.finder.letscheck.dto.SuggestionResponse;
import com.finder.letscheck.dto.SuggestionReviewRequest;
import com.finder.letscheck.model.Item;
import com.finder.letscheck.dto.ItemResponse;
import com.finder.letscheck.dto.ItemRequest;
import com.finder.letscheck.model.RewardTransaction;
import com.finder.letscheck.model.Suggestion;
import com.finder.letscheck.model.User;
import com.finder.letscheck.model.enums.RewardTransactionType;
import com.finder.letscheck.model.enums.SuggestionStatus;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.repository.RewardTransactionRepository;
import com.finder.letscheck.repository.SuggestionRepository;
import com.finder.letscheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContributionSuggestionService {

    private static final int REWARD_POINTS_APPROVED_NEW = 10;
    private static final int REWARD_POINTS_APPROVED_MERGED = 5;

    private final SuggestionRepository suggestionRepository;
    private final RewardTransactionRepository rewardTransactionRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    /**
     * Creates a new user suggestion and puts it into moderation queue.
     */
    public SuggestionResponse createSuggestion(SuggestionCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Suggestion suggestion = Suggestion.builder()
                .userId(request.getUserId())
                .itemName(request.getItemName())
                .normalizedItemName(normalize(request.getItemName()))
                .restaurantName(request.getRestaurantName())
                .normalizedRestaurantName(normalize(request.getRestaurantName()))
                .city(request.getCity())
                .normalizedCity(normalize(request.getCity()))
                .areaName(request.getAreaName())
                .normalizedAreaName(normalize(request.getAreaName()))
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .isVeg(request.getIsVeg())
                .note(request.getNote())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(SuggestionStatus.PENDING_REVIEW)
                .linkedItemId(null)
                .rewardPointsGranted(0)
                .reviewReason(null)
                .reviewedBy(null)
                .reviewedAt(null)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        suggestion = suggestionRepository.save(suggestion);

        user.setPendingContributionCount(
                safeInt(user.getPendingContributionCount()) + 1
        );
        userRepository.save(user);

        return mapToResponse(suggestion);
    }

    /**
     * Returns all suggestions submitted by a specific user.
     */
    public List<SuggestionResponse> getSuggestionsByUser(String userId) {
        return suggestionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns pending moderation queue.
     */
    public List<SuggestionResponse> getPendingSuggestions() {
        return suggestionRepository.findByStatusOrderByCreatedAtAsc(SuggestionStatus.PENDING_REVIEW)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Approves a suggestion as a brand new item.
     *
     * Important:
     * - This version assumes item creation will be plugged in later
     *   or done manually before linkedItemId is set.
     * - For now, linkedItemId can remain null if item creation is not yet automated.
     */
    public SuggestionResponse approveSuggestionAsNew(String suggestionId, SuggestionReviewRequest request) {
        Suggestion suggestion = getPendingSuggestionOrThrow(suggestionId);
        User user = getUserOrThrow(suggestion.getUserId());
        // 🔥 STEP 1: Create Item from suggestion by delegating to ItemService
        // We construct an ItemRequest and call addItem so restaurant creation,
        // duplicate checks and coordinate handling are consistent.
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setItemName(suggestion.getItemName());
        itemRequest.setCategory(suggestion.getCategory());
        itemRequest.setSubCategory(suggestion.getSubCategory());
        itemRequest.setPrice(suggestion.getPrice());
        itemRequest.setIsVeg(suggestion.getIsVeg());

        // Try to provide coordinates from suggestion if available (optional)
        itemRequest.setLatitude(suggestion.getLatitude());
        itemRequest.setLongitude(suggestion.getLongitude());

        // Populate restaurant fields so ItemService will create the restaurant
        itemRequest.setRestaurantName(suggestion.getRestaurantName());
        itemRequest.setAreaName(suggestion.getAreaName());
        itemRequest.setCity(suggestion.getCity());

        // Mark createdBy as system/moderator
        itemRequest.setCreatedBy(request.getReviewedBy());
        itemRequest.setCreatedByType("MODERATOR");

        ItemResponse createdItemResponse = itemService.addItem(itemRequest);
        Item item = itemRepository.findById(createdItemResponse.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created item"));

        // 🔥 STEP 2: Update suggestion
        suggestion.setStatus(SuggestionStatus.APPROVED_NEW);
        suggestion.setLinkedItemId(item.getId());
        suggestion.setRewardPointsGranted(REWARD_POINTS_APPROVED_NEW);
        suggestion.setReviewReason(request.getReviewReason());
        suggestion.setReviewedBy(request.getReviewedBy());
        suggestion.setReviewedAt(Instant.now().toString());
        suggestion.setUpdatedAt(Instant.now().toString());

        suggestion = suggestionRepository.save(suggestion);

        // 🔥 STEP 3: Reward
        createRewardTransaction(
                user.getId(),
                RewardTransactionType.SUGGESTION_APPROVED_NEW,
                suggestion.getId(),
                REWARD_POINTS_APPROVED_NEW,
                "Approved new food suggestion"
        );

        // 🔥 STEP 4: Update user
        user.setRewardPointsBalance(
                safeInt(user.getRewardPointsBalance()) + REWARD_POINTS_APPROVED_NEW
        );
        user.setApprovedContributionCount(
                safeInt(user.getApprovedContributionCount()) + 1
        );
        user.setPendingContributionCount(
                Math.max(0, safeInt(user.getPendingContributionCount()) - 1)
        );

        userRepository.save(user);

        return mapToResponse(suggestion);
    }

    /**
     * Approves a suggestion by merging it into an existing item.
     */
    public SuggestionResponse approveSuggestionAsMerged(String suggestionId, SuggestionReviewRequest request) {
        Suggestion suggestion = getPendingSuggestionOrThrow(suggestionId);
        User user = getUserOrThrow(suggestion.getUserId());

        if (request.getLinkedItemId() == null || request.getLinkedItemId().isBlank()) {
            throw new RuntimeException("linkedItemId is required for merged approval");
        }

        Item item = itemRepository.findById(request.getLinkedItemId())
                .orElseThrow(() -> new RuntimeException("Linked item not found"));

        // If suggestion had coordinates and the existing item has no location set,
        // apply suggestion coordinates to the item so location information is preserved.
        if ((suggestion.getLatitude() != null && suggestion.getLongitude() != null)
                && (item.getLocation() == null)) {
            com.finder.letscheck.model.Location loc = new com.finder.letscheck.model.Location(
                    "Point",
                    new double[]{suggestion.getLongitude(), suggestion.getLatitude()}
            );
            item.setLocation(loc);
            itemRepository.save(item);
        }

        suggestion.setStatus(SuggestionStatus.APPROVED_MERGED);
        suggestion.setLinkedItemId(item.getId());
        suggestion.setRewardPointsGranted(REWARD_POINTS_APPROVED_MERGED);
        suggestion.setReviewReason(request.getReviewReason());
        suggestion.setReviewedBy(request.getReviewedBy());
        suggestion.setReviewedAt(Instant.now().toString());
        suggestion.setUpdatedAt(Instant.now().toString());

        suggestion = suggestionRepository.save(suggestion);

        createRewardTransaction(
                user.getId(),
                RewardTransactionType.SUGGESTION_APPROVED_MERGED,
                suggestion.getId(),
                REWARD_POINTS_APPROVED_MERGED,
                "Approved merged food suggestion"
        );

        user.setRewardPointsBalance(
                safeInt(user.getRewardPointsBalance()) + REWARD_POINTS_APPROVED_MERGED
        );
        user.setApprovedContributionCount(
                safeInt(user.getApprovedContributionCount()) + 1
        );
        user.setPendingContributionCount(
                Math.max(0, safeInt(user.getPendingContributionCount()) - 1)
        );
        userRepository.save(user);

        return mapToResponse(suggestion);
    }

    /**
     * Rejects a pending suggestion.
     */
    public SuggestionResponse rejectSuggestion(String suggestionId, SuggestionReviewRequest request) {
        Suggestion suggestion = getPendingSuggestionOrThrow(suggestionId);
        User user = getUserOrThrow(suggestion.getUserId());

        suggestion.setStatus(SuggestionStatus.REJECTED);
        suggestion.setRewardPointsGranted(0);
        suggestion.setReviewReason(request.getReviewReason());
        suggestion.setReviewedBy(request.getReviewedBy());
        suggestion.setReviewedAt(Instant.now().toString());
        suggestion.setUpdatedAt(Instant.now().toString());

        suggestion = suggestionRepository.save(suggestion);

        user.setRejectedContributionCount(
                safeInt(user.getRejectedContributionCount()) + 1
        );
        user.setPendingContributionCount(
                Math.max(0, safeInt(user.getPendingContributionCount()) - 1)
        );
        userRepository.save(user);

        return mapToResponse(suggestion);
    }

    private Suggestion getPendingSuggestionOrThrow(String suggestionId) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        if (suggestion.getStatus() != SuggestionStatus.PENDING_REVIEW) {
            throw new RuntimeException("Only pending suggestions can be reviewed");
        }

        return suggestion;
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void createRewardTransaction(
            String userId,
            RewardTransactionType type,
            String referenceId,
            Integer points,
            String description
    ) {
        RewardTransaction transaction = RewardTransaction.builder()
                .userId(userId)
                .type(type)
                .referenceId(referenceId)
                .points(points)
                .description(description)
                .createdAt(Instant.now().toString())
                .build();

        rewardTransactionRepository.save(transaction);
    }

    private SuggestionResponse mapToResponse(Suggestion suggestion) {
        return SuggestionResponse.builder()
                .id(suggestion.getId())
                .userId(suggestion.getUserId())
                .itemName(suggestion.getItemName())
                .restaurantName(suggestion.getRestaurantName())
                .city(suggestion.getCity())
                .areaName(suggestion.getAreaName())
                .category(suggestion.getCategory())
                .subCategory(suggestion.getSubCategory())
                .price(suggestion.getPrice())
                .currency(suggestion.getCurrency())
                .isVeg(suggestion.getIsVeg())
                .note(suggestion.getNote())
                .status(suggestion.getStatus())
                .linkedItemId(suggestion.getLinkedItemId())
                .rewardPointsGranted(suggestion.getRewardPointsGranted())
                .reviewReason(suggestion.getReviewReason())
                .createdAt(suggestion.getCreatedAt())
                .reviewedAt(suggestion.getReviewedAt())
                .latitude(suggestion.getLatitude())
                .longitude(suggestion.getLongitude())
                .build();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}