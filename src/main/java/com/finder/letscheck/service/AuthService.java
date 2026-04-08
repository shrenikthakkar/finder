package com.finder.letscheck.service;

import com.finder.letscheck.dto.AuthRequest;
import com.finder.letscheck.dto.AuthResponse;
import com.finder.letscheck.dto.RegisterRequest;
import com.finder.letscheck.model.User;
import com.finder.letscheck.repository.UserRepository;
import com.finder.letscheck.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles registration and login business logic.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user.
     */
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email already registered");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.getPhoneNumber().trim())) {
            throw new RuntimeException("Phone number already registered");
        }

        User user = new User();

        // Basic identity
        user.setName(request.getName().trim());
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setPhoneNumber(normalizePhoneNumber(request.getPhoneNumber()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPublicUsername(resolvePublicUsername(request.getPublicUsername(), request.getName()));
        user.setRole("USER");

        // Default profile fields
        user.setIsActive(true);
        user.setIsProfilePublic(true);

        // Default counters
        user.setTotalReviewsGiven(0);
        user.setCitiesVisitedCount(0);
        user.setRewardPointsBalance(0);
        user.setApprovedContributionCount(0);
        user.setPendingContributionCount(0);
        user.setRejectedContributionCount(0);

        // Optional fields
        user.setCitiesVisited(List.of());

        // Timestamps
        String now = String.valueOf(System.currentTimeMillis());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        String subject = savedUser.getEmail() != null && !savedUser.getEmail().isBlank()
                ? savedUser.getEmail()
                : savedUser.getPhoneNumber();

        String token = jwtService.generateToken(subject, savedUser.getId(), savedUser.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole())
                .publicUsername(savedUser.getPublicUsername())
                .build();
    }

    private String resolvePublicUsername(String publicUsername, String name) {
        String base = (publicUsername != null && !publicUsername.isBlank())
                ? publicUsername
                : name;

        if (base == null || base.isBlank()) {
            return "spotzy_user";
        }

        String normalized = base.trim().toLowerCase()
                .replaceAll("[^a-z0-9._]", "_")
                .replaceAll("_+", "_");

        if (normalized.length() < 3) {
            normalized = normalized + "_user";
        }

        if (normalized.length() > 20) {
            normalized = normalized.substring(0, 20);
        }

        return normalized;
    }

    /**
     * Logs in an existing user.
     */
    public AuthResponse login(AuthRequest request) {
        if ((request.getEmail() == null || request.getEmail().isBlank())
                && (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            throw new RuntimeException("Email or phone number is required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }

        String email = normalizeEmail(request.getEmail());
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());

        User user = email != null
                ? userRepository.findByEmail(email).orElse(null)
                : userRepository.findByPhoneNumber(phoneNumber).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String subject = user.getEmail() != null && !user.getEmail().isBlank()
                ? user.getEmail()
                : user.getPhoneNumber();

        String token = jwtService.generateToken(subject, user.getId(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .publicUsername(user.getPublicUsername())
                .build();
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Name is required");
        }

        if ((request.getEmail() == null || request.getEmail().isBlank())
                && (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            throw new RuntimeException("Either email or phone number is required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }

        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return null;
        return phoneNumber.trim();
    }
}