package com.finder.letscheck.controller;

import com.finder.letscheck.dto.UserProfileSummaryResponse;
import com.finder.letscheck.dto.UserRequest;
import com.finder.letscheck.dto.UserResponse;
import com.finder.letscheck.security.CurrentUserService;
import com.finder.letscheck.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * User APIs.
 *
 * Launch direction:
 * - keep generic create/get endpoints if needed internally
 * - add authenticated "me" endpoints for app usage
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public UserResponse createUser(@RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable String id) {
        return userService.getUser(id);
    }

    @GetMapping("/{userId}/summary")
    public UserProfileSummaryResponse getUserSummary(@PathVariable String userId) {
        return userService.getUserSummary(userId);
    }

    /**
     * Get current logged-in user's profile.
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getUser(currentUserService.getCurrentUserId());
    }

    /**
     * Get current logged-in user's summary.
     */
    @GetMapping("/me/summary")
    public UserProfileSummaryResponse getCurrentUserSummary() {
        return userService.getUserSummary(currentUserService.getCurrentUserId());
    }
}