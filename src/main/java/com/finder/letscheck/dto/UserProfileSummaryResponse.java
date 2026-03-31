package com.finder.letscheck.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileSummaryResponse {

    private String userId;
    private String name;

    private Integer rewardPoints;

    private Integer approvedContributions;
    private Integer pendingContributions;
    private Integer rejectedContributions;
}