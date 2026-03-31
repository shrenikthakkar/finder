package com.finder.letscheck.model.enums;

/**
 * Represents the lifecycle state of a user suggestion.
 *
 * Flow:
 * PENDING_REVIEW -> APPROVED_NEW / APPROVED_MERGED / REJECTED
 */
public enum SuggestionStatus {
    PENDING_REVIEW,
    APPROVED_NEW,
    APPROVED_MERGED,
    REJECTED
}