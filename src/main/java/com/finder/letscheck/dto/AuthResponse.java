package com.finder.letscheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth response returned after register/login.
 *
 * Contains:
 * - JWT token
 * - basic user identity
 * - role for frontend-level decisions if needed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
}