package com.finder.letscheck.controller;

import com.finder.letscheck.dto.SuggestionCreateRequest;
import com.finder.letscheck.dto.SuggestionResponse;
import com.finder.letscheck.dto.SuggestionReviewRequest;
import com.finder.letscheck.service.ContributionSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final ContributionSuggestionService contributionSuggestionService;

    /**
     * User submits a new food suggestion.
     */
    @PostMapping
    public SuggestionResponse createSuggestion(@RequestBody SuggestionCreateRequest request) {
        return contributionSuggestionService.createSuggestion(request);
    }

    /**
     * Get all suggestions submitted by a specific user.
     */
    @GetMapping("/user/{userId}")
    public List<SuggestionResponse> getSuggestionsByUser(@PathVariable String userId) {
        return contributionSuggestionService.getSuggestionsByUser(userId);
    }

    /**
     * Admin queue: get all pending suggestions.
     */
    @GetMapping("/pending")
    public List<SuggestionResponse> getPendingSuggestions() {
        return contributionSuggestionService.getPendingSuggestions();
    }

    /**
     * Admin action: approve as new item.
     */
    @PutMapping("/{suggestionId}/approve-new")
    public SuggestionResponse approveSuggestionAsNew(
            @PathVariable String suggestionId,
            @RequestBody SuggestionReviewRequest request
    ) {
        return contributionSuggestionService.approveSuggestionAsNew(suggestionId, request);
    }

    /**
     * Admin action: approve by merging into an existing item.
     */
    @PutMapping("/{suggestionId}/approve-merged")
    public SuggestionResponse approveSuggestionAsMerged(
            @PathVariable String suggestionId,
            @RequestBody SuggestionReviewRequest request
    ) {
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
        return contributionSuggestionService.rejectSuggestion(suggestionId, request);
    }
}