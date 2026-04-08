package com.finder.letscheck.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {

    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private String bio;

    private Integer totalReviewsGiven;

    private List<String> citiesVisited;
    private Integer citiesVisitedCount;
    private String publicUsername;
}