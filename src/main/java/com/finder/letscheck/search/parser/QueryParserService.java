package com.finder.letscheck.search.parser;

import com.finder.letscheck.search.cache.SearchCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueryParserService {

    private static final double ITEM_THRESHOLD = 0.78;
    private static final double CITY_THRESHOLD = 0.88;
    private static final double AREA_THRESHOLD = 0.85;

    private static final Set<String> STOP_WORDS = Set.of(
            "best", "top", "famous", "popular", "must", "try", "near", "me",
            "nearby", "around", "in", "at", "find", "show", "food", "item", "items"
    );

    private final SearchCacheService searchCacheService;

    public QueryParseResult parse(String rawQuery) {
        String normalizedQuery = normalizeText(rawQuery);

        String city = resolveBestLocationMatch(normalizedQuery, searchCacheService.getNormalizedCities(), CITY_THRESHOLD);
        String area = resolveBestLocationMatch(normalizedQuery, searchCacheService.getNormalizedAreas(), AREA_THRESHOLD);

        String canonicalItem = resolveCanonicalItemFromSentence(normalizedQuery);

        boolean nearMeIntent = containsAny(normalizedQuery, "near me", "nearby", "around me");
        boolean areaIntent = area != null || city != null || normalizedQuery.contains(" in ");

        return QueryParseResult.builder()
                .originalQuery(rawQuery)
                .normalizedQuery(normalizedQuery)
                .canonicalItem(canonicalItem)
                .matchedItemToken(canonicalItem)
                .city(city)
                .area(area)
                .nearMeIntent(nearMeIntent)
                .areaIntent(areaIntent)
                .itemConfidence(canonicalItem != null ? 1.0 : 0.0)
                .cityConfidence(city != null ? 1.0 : 0.0)
                .areaConfidence(area != null ? 1.0 : 0.0)
                .build();
    }

    private String resolveCanonicalItemFromSentence(String normalizedQuery) {
        Map<String, String> canonicalMap = searchCacheService.getCanonicalMap();
        Map<String, String> aliasMap = searchCacheService.getAliasToCanonicalMap();

        // 1) exact full query canonical
        if (canonicalMap.containsKey(normalizedQuery)) {
            return canonicalMap.get(normalizedQuery);
        }

        // 2) exact full query alias
        if (aliasMap.containsKey(normalizedQuery)) {
            return aliasMap.get(normalizedQuery);
        }

        // 3) direct contains canonical phrase
        for (String canonical : canonicalMap.keySet()) {
            if (normalizedQuery.contains(canonical)) {
                return canonical;
            }
        }

        // 4) direct contains alias phrase
        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            if (normalizedQuery.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 5) generate candidate phrases and try exact/fuzzy
        List<String> phrases = generateCandidatePhrases(normalizedQuery);

        for (String phrase : phrases) {
            if (canonicalMap.containsKey(phrase)) {
                return canonicalMap.get(phrase);
            }
            if (aliasMap.containsKey(phrase)) {
                return aliasMap.get(phrase);
            }
        }

        // 6) fuzzy canonical
        String bestCanonical = null;
        double bestCanonicalScore = 0.0;
        for (String phrase : phrases) {
            String match = findBestFuzzyMatch(phrase, canonicalMap.keySet(), ITEM_THRESHOLD);
            if (match != null) {
                double score = similarity(phrase, match);
                if (score > bestCanonicalScore) {
                    bestCanonicalScore = score;
                    bestCanonical = match;
                }
            }
        }
        if (bestCanonical != null) {
            return bestCanonical;
        }

        // 7) fuzzy alias → canonical
        String bestAlias = null;
        double bestAliasScore = 0.0;
        for (String phrase : phrases) {
            String match = findBestFuzzyMatch(phrase, aliasMap.keySet(), ITEM_THRESHOLD);
            if (match != null) {
                double score = similarity(phrase, match);
                if (score > bestAliasScore) {
                    bestAliasScore = score;
                    bestAlias = match;
                }
            }
        }
        if (bestAlias != null) {
            return aliasMap.get(bestAlias);
        }

        return null;
    }

    private List<String> generateCandidatePhrases(String normalizedQuery) {
        String[] words = normalizedQuery.split("\\s+");
        List<String> filteredWords = Arrays.stream(words)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toList());

        List<String> phrases = new ArrayList<>();

        // full filtered sentence
        if (!filteredWords.isEmpty()) {
            phrases.add(String.join(" ", filteredWords));
        }

        // n-grams: 1, 2, 3 words
        for (int size = 1; size <= Math.min(3, filteredWords.size()); size++) {
            for (int i = 0; i <= filteredWords.size() - size; i++) {
                String phrase = String.join(" ", filteredWords.subList(i, i + size));
                phrases.add(phrase);
            }
        }

        // longer original substrings too
        for (int size = 2; size <= Math.min(4, words.length); size++) {
            for (int i = 0; i <= words.length - size; i++) {
                String phrase = String.join(" ", Arrays.copyOfRange(words, i, i + size));
                phrases.add(normalizeText(phrase));
            }
        }

        return phrases.stream().distinct().toList();
    }

    private String resolveBestLocationMatch(String normalizedQuery, Set<String> candidates, double threshold) {
        if (candidates.contains(normalizedQuery)) {
            return normalizedQuery;
        }

        for (String candidate : candidates) {
            if (normalizedQuery.contains(candidate)) {
                return candidate;
            }
        }

        // try phrase-level exact/contains first
        List<String> phrases = generateCandidatePhrases(normalizedQuery);
        for (String phrase : phrases) {
            if (candidates.contains(phrase)) {
                return phrase;
            }
            for (String candidate : candidates) {
                if (phrase.contains(candidate) || candidate.contains(phrase)) {
                    return candidate;
                }
            }
        }

        // fuzzy phrase-level match
        String bestMatch = null;
        double bestScore = 0.0;

        for (String phrase : phrases) {
            String candidate = findBestFuzzyMatch(phrase, candidates, threshold);
            if (candidate != null) {
                double score = similarity(phrase, candidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = candidate;
                }
            }
        }

        return bestMatch;
    }

    private String findBestFuzzyMatch(String input, Set<String> candidates, double threshold) {
        return candidates.stream()
                .map(candidate -> Map.entry(candidate, similarity(input, candidate)))
                .filter(entry -> entry.getValue() >= threshold)
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private double similarity(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0.0;
        }
        int maxLength = Math.max(a.length(), b.length());
        if (maxLength == 0) {
            return 1.0;
        }
        int distance = levenshteinDistance(a, b);
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}