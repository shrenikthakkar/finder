package com.finder.letscheck.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    /**
     * Filled by backend from authenticated JWT user.
     * Frontend should not send this.
     */
    private String userId;

    /**
     * Filled by backend from authenticated JWT user.
     * Frontend should not send this.
     */
    private String userName;

    @NotBlank(message = "Item id is required")
    private String itemId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private String comment;
}