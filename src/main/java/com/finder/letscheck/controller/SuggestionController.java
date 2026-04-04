package com.finder.letscheck.controller;

import com.finder.letscheck.dto.SuggestionCreateRequest;
import com.finder.letscheck.dto.SuggestionResponse;
import com.finder.letscheck.dto.SuggestionReviewRequest;
import com.finder.letscheck.security.CurrentUserService;
import com.finder.letscheck.service.ContributionSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Suggestion APIs.
 *
 * Rules:
 * - authenticated users can create suggestions and view their own suggestions
 * - admin-only APIs are used for moderation actions
 */
@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final ContributionSuggestionService contributionSuggestionService;
    private final CurrentUserService currentUserService;

    /**
     * User submits a new food suggestion.
     *
     * Security:
     * - ignore frontend-provided userId
     * - always assign current authenticated user
     */
    @PostMapping
    public SuggestionResponse createSuggestion(@RequestBody SuggestionCreateRequest request) {
        request.setUserId(currentUserService.getCurrentUserId());
        return contributionSuggestionService.createSuggestion(request);
    }

    /**
     * Get current logged-in user's suggestions.
     */
    @GetMapping("/me")
    public List<SuggestionResponse> getMySuggestions() {
        return contributionSuggestionService.getSuggestionsByUser(currentUserService.getCurrentUserId());
    }

    /**
     * Admin queue: get all pending suggestions.
     */
    @GetMapping("/pending")
    public List<SuggestionResponse> getPendingSuggestions() {
        currentUserService.requireAdmin();
        return contributionSuggestionService.getPendingSuggestions();
    }

    /**
     * Admin action: approve suggestion as a brand-new item.
     */
    @PutMapping("/{suggestionId}/approve-new")
    public SuggestionResponse approveSuggestionAsNew(
            @PathVariable String suggestionId,
            @RequestBody SuggestionReviewRequest request
    ) {
        currentUserService.requireAdmin();
        return contributionSuggestionService.approveSuggestionAsNew(suggestionId, request);
    }

    /**
     * Admin action: approve suggestion by merging into an existing item.
     */
    @PutMapping("/{suggestionId}/approve-merged")
    public SuggestionResponse approveSuggestionAsMerged(
            @PathVariable String suggestionId,
            @RequestBody SuggestionReviewRequest request
    ) {
        currentUserService.requireAdmin();
        return contributionSuggestionService.approveSuggestionAsMerged(suggestionId, request);
    }

    /**
     * Admin action: reject suggestion.
     */
    @PutMapping("/{suggestionId}/reject")
    public SuggestionResponse rejectSuggestion(
            @PathVariable String suggestionId,
            @RequestBody SuggestionReviewRequest request
    ) {
        currentUserService.requireAdmin();
        return contributionSuggestionService.rejectSuggestion(suggestionId, request);
    }
}