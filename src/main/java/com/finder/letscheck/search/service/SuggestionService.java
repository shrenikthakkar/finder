package com.finder.letscheck.search.service;

import com.finder.letscheck.search.cache.SearchCacheService;
import com.finder.letscheck.search.dto.SuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for building lightweight autocomplete suggestions.
 *
 * Current design goals:
 * - stay cache-based for speed and low DB cost
 * - support multi-word user queries
 * - support location tokens anywhere in the sentence
 * - support messy search inputs like:
 *   "items in ahmedabad"
 *   "good dabeli cheese"
 *   "food in mountain view"
 */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 10;

    /**
     * Common noise words that should not drive autocomplete matching.
     *
     * Why:
     * - users often type conversational search phrases
     * - we only want meaningful food / city / area words to influence suggestions
     */
    private static final Set<String> SUGGESTION_NOISE_WORDS = Set.of(
            "best", "top", "famous", "popular", "must", "try",
            "near", "me", "nearby", "around", "in", "at",
            "find", "show", "food", "item", "items",
            "good", "great", "nice", "please"
    );

    private final SearchCacheService searchCacheService;

    /**
     * Builds autocomplete suggestions for a partial or sentence-style query.
     *
     * Matching strategy:
     * - normalize raw query
     * - extract meaningful tokens
     * - score candidates using:
     *   1. exact full-query match
     *   2. prefix match
     *   3. contains match
     *   4. best token prefix/contains match
     *
     * This allows queries like:
     * - "items in ahmedabad" -> Ahmedabad
     * - "food in mountain view" -> Mountain View
     * - "good dabeli cheese" -> Dabeli / Cheese Dabeli style suggestions later
     */
    public List<SuggestionResponse> getSuggestions(String rawQuery, Integer requestedLimit) {
        String normalizedQuery = normalizeText(rawQuery);
        if (normalizedQuery == null || normalizedQuery.length() < 2) {
            return List.of();
        }

        int limit = getSafeLimit(requestedLimit);
        List<String> tokens = extractMeaningfulTokens(normalizedQuery);

        List<SuggestionResponse> suggestions = new ArrayList<>();

        suggestions.addAll(buildCanonicalSuggestions(normalizedQuery, tokens));
        suggestions.addAll(buildAliasSuggestions(normalizedQuery, tokens));
        suggestions.addAll(buildAreaSuggestions(normalizedQuery, tokens));
        suggestions.addAll(buildCitySuggestions(normalizedQuery, tokens));

        // Remove duplicates while keeping the highest scored suggestion.
        Map<String, SuggestionResponse> deduplicated = new HashMap<>();
        for (SuggestionResponse suggestion : suggestions) {
            String key = buildDeduplicationKey(suggestion);
            if (!deduplicated.containsKey(key)
                    || suggestion.getScore() > deduplicated.get(key).getScore()) {
                deduplicated.put(key, suggestion);
            }
        }

        return deduplicated.values().stream()
                .sorted(Comparator
                        .comparing(SuggestionResponse::getScore, Comparator.reverseOrder())
                        .thenComparing(SuggestionResponse::getDisplayText))
                .limit(limit)
                .toList();
    }

    /**
     * Builds suggestions for canonical food names.
     */
    private List<SuggestionResponse> buildCanonicalSuggestions(
            String normalizedQuery,
            List<String> tokens
    ) {
        return searchCacheService.getCanonicalMap().keySet().stream()
                .filter(candidate -> isSuggestionCandidateMatch(normalizedQuery, tokens, candidate))
                .map(candidate -> SuggestionResponse.builder()
                        .type("ITEM")
                        .displayText(toDisplayText(candidate))
                        .secondaryText(null)
                        .canonicalValue(candidate)
                        .area(null)
                        .city(null)
                        .score(calculateSuggestionScore(normalizedQuery, tokens, candidate, 100, 75, 68))
                        .build())
                .toList();
    }

    /**
     * Builds suggestions for alias names.
     */
    private List<SuggestionResponse> buildAliasSuggestions(
            String normalizedQuery,
            List<String> tokens
    ) {
        return searchCacheService.getAliasToCanonicalMap().entrySet().stream()
                .filter(entry -> isSuggestionCandidateMatch(normalizedQuery, tokens, entry.getKey()))
                .map(entry -> SuggestionResponse.builder()
                        .type("ALIAS")
                        .displayText(toDisplayText(entry.getKey()))
                        .secondaryText(toDisplayText(entry.getValue()))
                        .canonicalValue(entry.getValue())
                        .area(null)
                        .city(null)
                        .score(calculateSuggestionScore(normalizedQuery, tokens, entry.getKey(), 95, 70, 64))
                        .build())
                .toList();
    }

    /**
     * Builds suggestions for area names.
     */
    private List<SuggestionResponse> buildAreaSuggestions(
            String normalizedQuery,
            List<String> tokens
    ) {
        return searchCacheService.getNormalizedAreas().stream()
                .filter(area -> isSuggestionCandidateMatch(normalizedQuery, tokens, area))
                .map(area -> SuggestionResponse.builder()
                        .type("AREA")
                        .displayText(toDisplayText(area))
                        .secondaryText("Area")
                        .canonicalValue(null)
                        .area(area)
                        .city(null)
                        .score(calculateSuggestionScore(normalizedQuery, tokens, area, 90, 65, 60))
                        .build())
                .toList();
    }

    /**
     * Builds suggestions for city names.
     */
    private List<SuggestionResponse> buildCitySuggestions(
            String normalizedQuery,
            List<String> tokens
    ) {
        return searchCacheService.getNormalizedCities().stream()
                .filter(city -> isSuggestionCandidateMatch(normalizedQuery, tokens, city))
                .map(city -> SuggestionResponse.builder()
                        .type("CITY")
                        .displayText(toDisplayText(city))
                        .secondaryText("City")
                        .canonicalValue(null)
                        .area(null)
                        .city(city)
                        .score(calculateSuggestionScore(normalizedQuery, tokens, city, 85, 60, 56))
                        .build())
                .toList();
    }

    /**
     * Checks whether a suggestion candidate should be considered at all.
     *
     * Match rules:
     * - full query prefix or contains
     * - OR any meaningful token prefix/contains
     */
    private boolean isSuggestionCandidateMatch(
            String normalizedQuery,
            List<String> tokens,
            String candidate
    ) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }

        if (candidate.startsWith(normalizedQuery) || candidate.contains(normalizedQuery)) {
            return true;
        }

        return tokens.stream().anyMatch(token ->
                candidate.startsWith(token) || candidate.contains(token)
        );
    }

    /**
     * Calculates ranking score for one suggestion candidate.
     *
     * Priority:
     * - full-query prefix
     * - full-query contains
     * - token prefix
     * - token contains
     */
    private int calculateSuggestionScore(
            String normalizedQuery,
            List<String> tokens,
            String candidate,
            int fullPrefixScore,
            int fullContainsScore,
            int tokenScore
    ) {
        if (candidate.startsWith(normalizedQuery)) {
            return fullPrefixScore;
        }

        if (candidate.contains(normalizedQuery)) {
            return fullContainsScore;
        }

        boolean tokenPrefix = tokens.stream().anyMatch(candidate::startsWith);
        if (tokenPrefix) {
            return tokenScore;
        }

        boolean tokenContains = tokens.stream().anyMatch(candidate::contains);
        if (tokenContains) {
            return tokenScore - 4;
        }

        return 0;
    }

    /**
     * Extracts useful tokens from sentence-style autocomplete queries.
     *
     * Example:
     * - "items in ahmedabad" -> ["ahmedabad"]
     * - "good dabeli cheese" -> ["dabeli", "cheese"]
     */
    private List<String> extractMeaningfulTokens(String normalizedQuery) {
        return Arrays.stream(normalizedQuery.split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() >= 2)
                .filter(token -> !SUGGESTION_NOISE_WORDS.contains(token))
                .distinct()
                .toList();
    }

    /**
     * Builds a deduplication key for suggestions.
     *
     * Why:
     * - avoids duplicate entries in the response
     * - keeps the best scored version when a value appears from multiple sources
     */
    private String buildDeduplicationKey(SuggestionResponse suggestion) {
        return suggestion.getType() + "|" + suggestion.getDisplayText() + "|" + suggestion.getCanonicalValue();
    }

    /**
     * Returns a safe result limit.
     */
    private int getSafeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    /**
     * Normalizes incoming text for matching.
     */
    private String normalizeText(String value) {
        return value == null ? null : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Converts normalized text to display-friendly format.
     *
     * Example:
     * "pani puri" -> "Pani Puri"
     */
    private String toDisplayText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return Arrays.stream(value.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}