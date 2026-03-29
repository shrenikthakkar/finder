package com.finder.letscheck.dto;

import lombok.Data;

@Data
public class UserRequest {

    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private String bio;
}