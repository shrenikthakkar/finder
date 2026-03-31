package com.finder.letscheck.service;

import com.finder.letscheck.dto.UserProfileSummaryResponse;
import com.finder.letscheck.dto.UserRequest;
import com.finder.letscheck.dto.UserResponse;
import com.finder.letscheck.model.User;
import com.finder.letscheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Creates a new user.
     */
    public UserResponse createUser(UserRequest request) {

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .profileImageUrl(request.getProfileImageUrl())
                .bio(request.getBio())
                .totalReviewsGiven(0)
                .citiesVisited(new ArrayList<>())
                .citiesVisitedCount(0)
                .role("USER")
                .isActive(true)
                .isProfilePublic(true)
                .rewardPointsBalance(0)
                .approvedContributionCount(0)
                .pendingContributionCount(0)
                .rejectedContributionCount(0)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    /**
     * Fetch user by ID
     */
    public UserResponse getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .totalReviewsGiven(user.getTotalReviewsGiven())
                .citiesVisited(user.getCitiesVisited())
                .citiesVisitedCount(user.getCitiesVisitedCount())
                .build();
    }

    public UserProfileSummaryResponse getUserSummary(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileSummaryResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .rewardPoints(safeInt(user.getRewardPointsBalance()))
                .approvedContributions(safeInt(user.getApprovedContributionCount()))
                .pendingContributions(safeInt(user.getPendingContributionCount()))
                .rejectedContributions(safeInt(user.getRejectedContributionCount()))
                .build();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}