package com.finder.letscheck.controller;

import com.finder.letscheck.dto.AuthRequest;
import com.finder.letscheck.dto.AuthResponse;
import com.finder.letscheck.dto.RegisterRequest;
import com.finder.letscheck.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication APIs.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register new user.
     */
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Login existing user.
     */
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }
}