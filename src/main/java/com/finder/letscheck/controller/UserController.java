package com.finder.letscheck.controller;

import com.finder.letscheck.dto.UserProfileSummaryResponse;
import com.finder.letscheck.dto.UserRequest;
import com.finder.letscheck.dto.UserResponse;
import com.finder.letscheck.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}