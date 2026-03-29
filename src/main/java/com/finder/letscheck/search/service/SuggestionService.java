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
 * V1 scope:
 * - canonical food suggestions
 * - alias suggestions
 * - area suggestions
 * - city suggestions
 *
 * Why cache-based first:
 * - very fast
 * - low latency
 * - no DB query on every keystroke
 * - scalable enough for current stage
 */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 10;

    private final SearchCacheService searchCacheService;

    /**
     * Builds suggestions for a partial user query.
     *
     * Improvement:
     * - helps users before full search
     * - reduces typo-driven failed searches
     * - uses cache for low latency
     */
    public List<SuggestionResponse> getSuggestions(String rawQuery, Integer requestedLimit) {

        String normalizedQuery = normalizeText(rawQuery);

        if (normalizedQuery == null || normalizedQuery.length() < 2) {
            return List.of();
        }

        int limit = getSafeLimit(requestedLimit);

        List<SuggestionResponse> suggestions = new ArrayList<>();

        // Add canonical food suggestions
        suggestions.addAll(buildCanonicalSuggestions(normalizedQuery));

        // Add alias suggestions
        suggestions.addAll(buildAliasSuggestions(normalizedQuery));

        // Add area suggestions
        suggestions.addAll(buildAreaSuggestions(normalizedQuery));

        // Add city suggestions
        suggestions.addAll(buildCitySuggestions(normalizedQuery));

        // Remove duplicates while keeping the best-scored one
        Map<String, SuggestionResponse> deduplicated = new HashMap<>();
        for (SuggestionResponse suggestion : suggestions) {
            String key = buildDeduplicationKey(suggestion);

            if (!deduplicated.containsKey(key)
                    || suggestion.getScore() > deduplicated.get(key).getScore()) {
                deduplicated.put(key, suggestion);
            }
        }

        // Sort by score descending, then display text ascending for stable order
        return deduplicated.values().stream()
                .sorted(Comparator
                        .comparing(SuggestionResponse::getScore, Comparator.reverseOrder())
                        .thenComparing(SuggestionResponse::getDisplayText))
                .limit(limit)
                .toList();
    }

    /**
     * Builds suggestions for canonical food names.
     *
     * Example:
     * query = "pan"
     * suggestion = "Pani Puri"
     */
    private List<SuggestionResponse> buildCanonicalSuggestions(String normalizedQuery) {
        return searchCacheService.getCanonicalMap().keySet().stream()
                .filter(candidate -> candidate.startsWith(normalizedQuery) || candidate.contains(normalizedQuery))
                .map(candidate -> SuggestionResponse.builder()
                        .type("ITEM")
                        .displayText(toDisplayText(candidate))
                        .secondaryText(null)
                        .canonicalValue(candidate)
                        .area(null)
                        .city(null)
                        .score(calculateTextMatchScore(normalizedQuery, candidate, 100, 75))
                        .build())
                .toList();
    }

    /**
     * Builds suggestions for alias names.
     *
     * Example:
     * query = "gol"
     * suggestion = "Golgappa" with canonical value "pani puri"
     */
    private List<SuggestionResponse> buildAliasSuggestions(String normalizedQuery) {
        return searchCacheService.getAliasToCanonicalMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(normalizedQuery) || entry.getKey().contains(normalizedQuery))
                .map(entry -> SuggestionResponse.builder()
                        .type("ALIAS")
                        .displayText(toDisplayText(entry.getKey()))
                        .secondaryText(toDisplayText(entry.getValue()))
                        .canonicalValue(entry.getValue())
                        .area(null)
                        .city(null)
                        .score(calculateTextMatchScore(normalizedQuery, entry.getKey(), 95, 70))
                        .build())
                .toList();
    }

    /**
     * Builds suggestions for area names.
     *
     * Example:
     * query = "nav"
     * suggestion = "Navrangpura"
     */
    private List<SuggestionResponse> buildAreaSuggestions(String normalizedQuery) {
        return searchCacheService.getNormalizedAreas().stream()
                .filter(area -> area.startsWith(normalizedQuery) || area.contains(normalizedQuery))
                .map(area -> SuggestionResponse.builder()
                        .type("AREA")
                        .displayText(toDisplayText(area))
                        .secondaryText("Area")
                        .canonicalValue(null)
                        .area(area)
                        .city(null)
                        .score(calculateTextMatchScore(normalizedQuery, area, 90, 65))
                        .build())
                .toList();
    }

    /**
     * Builds suggestions for city names.
     *
     * Example:
     * query = "ah"
     * suggestion = "Ahmedabad"
     */
    private List<SuggestionResponse> buildCitySuggestions(String normalizedQuery) {
        return searchCacheService.getNormalizedCities().stream()
                .filter(city -> city.startsWith(normalizedQuery) || city.contains(normalizedQuery))
                .map(city -> SuggestionResponse.builder()
                        .type("CITY")
                        .displayText(toDisplayText(city))
                        .secondaryText("City")
                        .canonicalValue(null)
                        .area(null)
                        .city(city)
                        .score(calculateTextMatchScore(normalizedQuery, city, 85, 60))
                        .build())
                .toList();
    }

    /**
     * Calculates a simple match score.
     *
     * Why:
     * - prefix matches should rank higher than contains matches
     * - keeps suggestion ranking intuitive
     */
    private int calculateTextMatchScore(String query, String candidate, int prefixScore, int containsScore) {
        if (candidate.startsWith(query)) {
            return prefixScore;
        }
        return containsScore;
    }

    /**
     * Builds a deduplication key for suggestions.
     *
     * Why:
     * - avoids duplicate entries in response
     * - for example same canonical food appearing from multiple sources
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