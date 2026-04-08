package com.finder.letscheck.controller;

import com.finder.letscheck.dto.PublicUsernameUpdateRequest;
import com.finder.letscheck.dto.UserResponse;
import com.finder.letscheck.model.User;
import com.finder.letscheck.repository.UserRepository;
import com.finder.letscheck.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserResponse getCurrentAccount() {
        User user = currentUserService.getCurrentUser();
        return mapToResponse(user);
    }

    @PutMapping("/me/public-username")
    public UserResponse updatePublicUsername(@RequestBody PublicUsernameUpdateRequest request) {
        User user = currentUserService.getCurrentUser();

        String normalized = normalizePublicUsername(request.getPublicUsername());
        if (normalized == null) {
            throw new RuntimeException("Public username is required");
        }

        user.setPublicUsername(normalized);
        user.setUpdatedAt(Instant.now().toString());

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .publicUsername(resolvePublicUsername(user))
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .totalReviewsGiven(user.getTotalReviewsGiven())
                .citiesVisited(user.getCitiesVisited())
                .citiesVisitedCount(user.getCitiesVisitedCount())
                .build();
    }

    private String resolvePublicUsername(User user) {
        if (user.getPublicUsername() != null && !user.getPublicUsername().isBlank()) {
            return user.getPublicUsername().trim();
        }

        String base = user.getName() != null && !user.getName().isBlank()
                ? user.getName().trim()
                : "spotzy_user";

        return sanitizeFallback(base);
    }

    private String normalizePublicUsername(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase()
                .replaceAll("[^a-z0-9._]", "_")
                .replaceAll("_+", "_");

        if (normalized.length() < 3) {
            throw new RuntimeException("Public username must be at least 3 characters");
        }

        if (normalized.length() > 20) {
            throw new RuntimeException("Public username must be at most 20 characters");
        }

        return normalized;
    }

    private String sanitizeFallback(String value) {
        String normalized = value.toLowerCase()
                .replaceAll("[^a-z0-9._]", "_")
                .replaceAll("_+", "_");

        if (normalized.isBlank()) {
            return "spotzy_user";
        }

        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }
}