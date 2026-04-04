package com.finder.letscheck.security;

import com.finder.letscheck.model.User;
import com.finder.letscheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Helper service to access the currently authenticated user.
 *
 * Why this exists:
 * - controllers/services should not trust raw userId from frontend
 * - JWT + Spring Security already know who the caller is
 * - centralizing this logic avoids repeating auth parsing everywhere
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Returns the currently authenticated application's User document.
     *
     * Authentication name is expected to be:
     * - email
     * - or phone number
     *
     * That matches how JWT subject was generated during login/register.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No authenticated user found");
        }

        String username = authentication.getName();

        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhoneNumber(username))
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    /**
     * Convenience method for current user id.
     */
    public String getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Checks whether current authenticated user has the given role.
     *
     * Example:
     * - hasRole("ADMIN")
     * - hasRole("USER")
     */
    public boolean hasRole(String role) {
        User user = getCurrentUser();
        return user.getRole() != null && user.getRole().equalsIgnoreCase(role);
    }

    /**
     * Ensures current authenticated user has ADMIN role.
     *
     * Throws runtime exception if not allowed.
     * This is a simple launch-safe approach before adding custom exception hierarchy.
     */
    public void requireAdmin() {
        if (!hasRole("ADMIN")) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
    }
}