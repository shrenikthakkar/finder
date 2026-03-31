package com.finder.letscheck.dto;

import lombok.Data;

/**
 * Request DTO for admin moderation actions.
 *
 * Used for:
 * - approve as new
 * - approve as merged
 * - reject
 */
@Data
public class SuggestionReviewRequest {

    /**
     * Admin identifier/name for audit.
     */
    private String reviewedBy;

    /**
     * Optional reason or note from reviewer.
     */
    private String reviewReason;

    /**
     * Required only for approve-as-merged flow.
     */
    private String linkedItemId;
}