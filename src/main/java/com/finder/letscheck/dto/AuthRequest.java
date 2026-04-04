package com.finder.letscheck.dto;

import lombok.Data;

/**
 * Login request.
 *
 * User can log in using either:
 * - email + password
 * - phoneNumber + password
 */
@Data
public class AuthRequest {
    private String email;
    private String phoneNumber;
    private String password;
}