package com.finder.letscheck.dto;

import lombok.Data;

/**
 * Registration request.
 *
 * Launch version:
 * - name required
 * - password required
 * - either email or phone required
 */
@Data
public class RegisterRequest {
    private String name;
    private String publicUsername;
    private String email;
    private String phoneNumber;
    private String password;
}