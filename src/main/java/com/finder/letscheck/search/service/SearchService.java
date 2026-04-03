package com.finder.letscheck.search.service;

import com.finder.letscheck.model.Item;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.repository.UserBucketListRepository;
import com.finder.letscheck.search.dto.ItemSearchRequest;
import com.finder.letscheck.search.dto.ItemSearchResponse;
import com.finder.letscheck.search.parser.QueryParserService;
import com.finder.letscheck.service.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import com.finder.letscheck.search.parser.QueryParseResult;
import com.finder.letscheck.search.parser.QueryParserService;
import com.finder.letscheck.model.UserBucketList;
import com.finder.letscheck.repository.UserBucketListRepository;
import com.finder.letscheck.dto.SearchAnalyticsRequest;
import com.finder.letscheck.model.enums.SearchSource;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final QueryParserService queryParserService;
    private final MongoTemplate mongoTemplate;
    private final ItemRepository itemRepository;
    private final UserBucketListRepository userBucketListRepository;
    private final SearchAnalyticsService searchAnalyticsService;

    private static final double SEARCH_FALLBACK_THRESHOLD = 0.50;
    private static final int SMART_CANDIDATE_LIMIT_CAP = 100;

    private static final Set<String> SEARCH_NOISE_WORDS = Set.of(
            "best", "top", "famous", "popular", "must", "try",
            "near", "me", "nearby", "around", "in", "at",
            "find", "show", "food", "item", "items",
            "good", "great", "nice", "please"
    );

    private static final List<String> NEAR_ME_PHRASES = List.of(
            "near me",
            "near by me",
            "nearby me",
            "nearby",
            "around me",
            "around",
            "close to me",
            "close by me",
            "near my location"
    );

    /**
     * Searches nearby active items using MongoDB geo filtering.
     *
     * Improvement:
     * - uses geo index for fast nearby search
     * - applies result limit for better scalability
     */
    public List<ItemSearchResponse> searchNearbyItems(ItemSearchRequest request) {
        validateNearbySearchRequest(request);

        int limit = getSafeLimit(request.getLimit());

        Point userLocation = new Point(request.getLongitude(), request.getLatitude());
        Distance radius = new Distance(request.getRadiusInKm(), Metrics.KILOMETERS);

        Query query = new Query();

        // Geo filter for nearby results
        query.addCriteria(
                Criteria.where("location").nearSphere(userLocation).maxDistance(radius.getNormalizedValue())
        );

        // Only active items should be returned
        query.addCriteria(Criteria.where("isActive").is(true));

        // Restrict number of fetched records
        query.limit(limit);

        List<Item> items = mongoTemplate.find(query, Item.class);

        List<ItemSearchResponse> results = items.stream()
                .map(item -> mapToSearchResponse(item, request.getLatitude(), request.getLongitude()))
                .sorted(defaultSearchComparator())
                .toList();

        searchAnalyticsService.logSearchAsync(
                SearchAnalyticsRequest.builder()
                        .query("nearby_search")
                        .normalizedQuery("nearby_search")
                        .userId(request.getUserId())
                        .city(null)
                        .areaName(null)
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .source(SearchSource.NEARBY_SEARCH)
                        .resultCount(results.size())
                        .build()
        );

        return attachBookmarkStatus(results, request.getUserId());
    }

    /**
     * Searches nearby active items for a specific normalized item query.
     *
     * Improvement:
     * - Uses geo filtering + exact normalized item match
     * - Limits results for better performance
     */
    public List<ItemSearchResponse> searchNearbyItemsByQuery(ItemSearchRequest request) {
        validateNearbySearchRequest(request);

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new RuntimeException("Query is required for nearby query search");
        }

        int limit = getSafeLimit(request.getLimit());
        int candidateLimit = getSmartCandidateLimit(limit);

        Query query = new Query();
        Point userLocation = new Point(request.getLongitude(), request.getLatitude());
        Distance radius = new Distance(request.getRadiusInKm(), Metrics.KILOMETERS);

        query.addCriteria(
                Criteria.where("location")
                        .nearSphere(userLocation)
                        .maxDistance(radius.getNormalizedValue())
        );
        query.addCriteria(Criteria.where("isActive").is(true));

        List<String> tokens = extractMeaningfulTokens(request.getQuery());
        Criteria tokenCriteria = buildAnyTokenRegexCriteria(tokens);
        if (tokenCriteria != null) {
            query.addCriteria(tokenCriteria);
        }

        query.limit(candidateLimit);

        List<Item> items = mongoTemplate.find(query, Item.class);
        List<Item> matchedItems = applySmartItemMatching(items, request.getQuery(), limit);

        List<ItemSearchResponse> results = matchedItems.stream()
                .map(item -> mapToSearchResponse(item, request.getLatitude(), request.getLongitude()))
                .sorted(defaultSearchComparator())
                .toList();

        searchAnalyticsService.logSearchAsync(
                SearchAnalyticsRequest.builder()
                        .query(request.getQuery())
                        .normalizedQuery(normalize(request.getQuery()))
                        .userId(request.getUserId())
                        .city(null)
                        .areaName(null)
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .source(SearchSource.NEARBY_QUERY_SEARCH)
                        .resultCount(results.size())
                        .build()
        );

        return attachBookmarkStatus(results, request.getUserId());
    }

    /**
     * Searches items by city / area / item query using MongoDB filtering
     * instead of loading all items in memory and filtering in Java.
     *
     * Improvement:
     * - Better performance for large datasets
     * - Less memory usage in backend
     * - Better scalability
     */
    /**
     * Searches active items by city / area / item query using MongoDB filtering.
     *
     * Improvement:
     * - Avoids loading all items into memory
     * - Uses database filtering for scale
     * - Applies result limits for performance stability
     */
    public List<ItemSearchResponse> searchItemsByArea(ItemSearchRequest request) {
        if ((request.getCity() == null || request.getCity().isBlank())
                && (request.getArea() == null || request.getArea().isBlank())) {
            throw new RuntimeException("Either city or area is required for area search");
        }

        int limit = getSafeLimit(request.getLimit());
        int candidateLimit = getSmartCandidateLimit(limit);

        String normalizedCity = request.getCity() != null ? normalizeText(request.getCity()) : null;
        String normalizedArea = request.getArea() != null ? normalizeText(request.getArea()) : null;

        Query mongoQuery = new Query();
        mongoQuery.addCriteria(Criteria.where("isActive").is(true));

        if (normalizedCity != null) {
            mongoQuery.addCriteria(Criteria.where("normalizedCity").is(normalizedCity));
        }

        if (normalizedArea != null) {
            mongoQuery.addCriteria(Criteria.where("normalizedAreaName").is(normalizedArea));
        }

        List<String> tokens = extractMeaningfulTokens(request.getQuery());
        Criteria tokenCriteria = buildAnyTokenRegexCriteria(tokens);
        if (tokenCriteria != null) {
            mongoQuery.addCriteria(tokenCriteria);
        }

        mongoQuery.limit(candidateLimit);

        List<Item> items = mongoTemplate.find(mongoQuery, Item.class);

        List<Item> matchedItems = request.getQuery() == null || request.getQuery().isBlank()
                ? items.stream()
                .sorted(itemSearchComparator())
                .limit(limit)
                .toList()
                : applySmartItemMatching(items, request.getQuery(), limit);

        List<ItemSearchResponse> results = matchedItems.stream()
                .map(item -> mapToSearchResponse(item, null, null))
                .sorted(defaultSearchComparator())
                .toList();

        searchAnalyticsService.logSearchAsync(
                SearchAnalyticsRequest.builder()
                        .query(request.getQuery())
                        .normalizedQuery(normalize(request.getQuery()))
                        .userId(request.getUserId())
                        .city(request.getCity())
                        .areaName(request.getArea())
                        .latitude(null)
                        .longitude(null)
                        .source(SearchSource.AREA_SEARCH)
                        .resultCount(results.size())
                        .build()
        );

        return attachBookmarkStatus(results, request.getUserId());
    }

    /**
     * Converts Item entity to search response DTO.
     *
     * Note:
     * - bookmark status is filled later in bulk
     */
    private ItemSearchResponse mapToSearchResponse(Item item, Double userLat, Double userLng) {
        Double distanceInKm = null;

        if (userLat != null && userLng != null
                && item.getLocation() != null
                && item.getLocation().getCoordinates() != null
                && item.getLocation().getCoordinates().length == 2) {

            double itemLng = item.getLocation().getCoordinates()[0];
            double itemLat = item.getLocation().getCoordinates()[1];

            distanceInKm = calculateDistanceInKm(userLat, userLng, itemLat, itemLng);
        }

        return ItemSearchResponse.builder()
                .itemId(item.getId())
                .itemName(item.getItemName())
                .restaurantId(item.getRestaurantId())
                .restaurantName(item.getRestaurantName())
                .city(item.getCity())
                .areaName(item.getAreaName())
                .avgItemRating(item.getAvgItemRating())
                .ratingCount(item.getRatingCount())
                .distanceInKm(distanceInKm)
                .isBookmarked(false)
                .build();
    }

    private Comparator<ItemSearchResponse> defaultSearchComparator() {
        return Comparator
                .comparing(ItemSearchResponse::getAvgItemRating,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ItemSearchResponse::getRatingCount,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ItemSearchResponse::getDistanceInKm,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private void validateNearbySearchRequest(ItemSearchRequest request) {
        if (request.getLatitude() == null) {
            throw new RuntimeException("Latitude is required");
        }
        if (request.getLongitude() == null) {
            throw new RuntimeException("Longitude is required");
        }
        if (request.getRadiusInKm() == null || request.getRadiusInKm() <= 0) {
            throw new RuntimeException("Radius in km must be greater than 0");
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371.0;

        double latDiff = Math.toRadians(lat2 - lat1);
        double lonDiff = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Smart search entry point for raw user queries.
     *
     * Improvement:
     * - Parses user sentence into structured search input
     * - Chooses best search strategy
     * - Applies safe result limits for stable performance
     */
    public List<ItemSearchResponse> smartSearch(
            String rawQuery,
            Double latitude,
            Double longitude,
            Double radiusInKm,
            String userId
    ) {
        QueryParseResult parsed = queryParserService.parse(rawQuery);

        System.out.println(
                "Parsed query => item: " + parsed.getCanonicalItem()
                        + ", city: " + parsed.getCity()
                        + ", area: " + parsed.getArea()
                        + ", nearMe: " + parsed.isNearMeIntent()
        );

        String effectiveSearchText = deriveEffectiveSearchText(rawQuery, parsed);

        // 1) Prefer area/city search when a location is parsed from query.
        if (parsed.getArea() != null || parsed.getCity() != null) {
            ItemSearchRequest request = new ItemSearchRequest();
            request.setCity(parsed.getCity());
            request.setArea(parsed.getArea());
            request.setQuery(effectiveSearchText);
            request.setLimit(20);
            request.setUserId(userId);
            return searchItemsByArea(request);
        }

        // 2) Nearby search when user intent is near-me or coordinates are present.
        if (parsed.isNearMeIntent() || (latitude != null && longitude != null)) {
            if (latitude != null && longitude != null) {
                ItemSearchRequest request = new ItemSearchRequest();
                request.setLatitude(latitude);
                request.setLongitude(longitude);
                request.setRadiusInKm(radiusInKm != null ? radiusInKm : 5.0);
                request.setQuery(effectiveSearchText);
                request.setLimit(20);
                request.setUserId(userId);

                if (effectiveSearchText != null && !effectiveSearchText.isBlank()) {
                    return searchNearbyItemsByQuery(request);
                }

                return searchNearbyItems(request);
            }
            // no location available -> fall through to item-only fallback
        }

        // 3) Item-only fallback with smart partial/prefix/fuzzy matching.
        if (effectiveSearchText != null && !effectiveSearchText.isBlank()) {
            int limit = 20;
            int candidateLimit = getSmartCandidateLimit(limit);

            Query mongoQuery = new Query();
            mongoQuery.addCriteria(Criteria.where("isActive").is(true));

            List<String> tokens = extractMeaningfulTokens(effectiveSearchText);
            Criteria tokenCriteria = buildAnyTokenRegexCriteria(tokens);
            if (tokenCriteria != null) {
                mongoQuery.addCriteria(tokenCriteria);
            }

            mongoQuery.limit(candidateLimit);

            List<Item> items = mongoTemplate.find(mongoQuery, Item.class);
            List<Item> matchedItems = applySmartItemMatching(items, effectiveSearchText, limit);

            List<ItemSearchResponse> results = matchedItems.stream()
                    .map(item -> mapToSearchResponse(item, latitude, longitude))
                    .sorted(defaultSearchComparator())
                    .toList();

            searchAnalyticsService.logSearchAsync(
                    SearchAnalyticsRequest.builder()
                            .query(rawQuery)
                            .normalizedQuery(normalize(rawQuery))
                            .userId(userId)
                            .city(null)
                            .areaName(null)
                            .latitude(latitude)
                            .longitude(longitude)
                            .source(SearchSource.SMART_SEARCH)
                            .resultCount(results.size())
                            .build()
            );

            return attachBookmarkStatus(results, userId);
        }

        return List.of();
    }

    /**
     * Returns a safe result limit.
     *
     * Why:
     * - prevents huge result sets from hurting performance
     * - gives a default when limit is missing
     * - caps the max allowed results
     */
    private int getSafeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return 20;
        }
        return Math.min(requestedLimit, 50);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private int getSmartCandidateLimit(Integer requestedLimit) {
        return Math.min(Math.max(getSafeLimit(requestedLimit) * 5, 40), SMART_CANDIDATE_LIMIT_CAP);
    }

    private Comparator<Item> itemSearchComparator() {
        return Comparator
                .comparing(Item::getAvgItemRating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Item::getRatingCount, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String deriveEffectiveSearchText(String rawQuery, QueryParseResult parsed) {
        if (parsed.getCanonicalItem() != null && !parsed.getCanonicalItem().isBlank()) {
            return parsed.getCanonicalItem();
        }

        String normalized = normalizeText(rawQuery);

        for (String phrase : NEAR_ME_PHRASES) {
            normalized = normalized.replace(phrase, " ");
        }

        if (parsed.getCity() != null && !parsed.getCity().isBlank()) {
            normalized = normalized.replace(parsed.getCity(), " ");
        }

        if (parsed.getArea() != null && !parsed.getArea().isBlank()) {
            normalized = normalized.replace(parsed.getArea(), " ");
        }

        List<String> tokens = extractMeaningfulTokens(normalized);
        if (tokens.isEmpty()) {
            return null;
        }

        return String.join(" ", tokens);
    }

    private List<String> extractMeaningfulTokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalizeText(text).split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> !SEARCH_NOISE_WORDS.contains(token))
                .filter(token -> token.length() >= 2)
                .distinct()
                .toList();
    }

    private Criteria buildAnyTokenRegexCriteria(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        List<Criteria> tokenCriterias = tokens.stream()
                .map(token -> Criteria.where("normalizedItemName")
                        .regex(".*" + Pattern.quote(token) + ".*", "i"))
                .toList();

        return new Criteria().orOperator(tokenCriterias.toArray(new Criteria[0]));
    }

    private List<Item> applySmartItemMatching(List<Item> items, String searchText, int limit) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        String normalizedSearchText = normalizeText(searchText);
        List<String> tokens = extractMeaningfulTokens(searchText);

        if ((normalizedSearchText == null || normalizedSearchText.isBlank()) && tokens.isEmpty()) {
            return items.stream()
                    .sorted(itemSearchComparator())
                    .limit(limit)
                    .toList();
        }

        return items.stream()
                .map(item -> Map.entry(item, computeMatchScore(item, normalizedSearchText, tokens)))
                .filter(entry -> entry.getValue() >= SEARCH_FALLBACK_THRESHOLD)
                .sorted(
                        Comparator.<Map.Entry<Item, Double>>comparingDouble(Map.Entry::getValue).reversed()
                                .thenComparing(entry -> entry.getKey().getAvgItemRating(),
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(entry -> entry.getKey().getRatingCount(),
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .map(Map.Entry::getKey)
                .limit(limit)
                .toList();
    }

    private double computeMatchScore(Item item, String normalizedSearchText, List<String> tokens) {
        String itemName = normalizeText(
                item.getNormalizedItemName() != null ? item.getNormalizedItemName() : item.getItemName()
        );

        if (itemName == null || itemName.isBlank()) {
            return 0.0;
        }

        if (normalizedSearchText != null && !normalizedSearchText.isBlank()) {
            if (itemName.equals(normalizedSearchText)) {
                return 1.00;
            }
            if (itemName.startsWith(normalizedSearchText)) {
                return 0.95;
            }
            if (itemName.contains(normalizedSearchText)) {
                return 0.90;
            }
        }

        if (!tokens.isEmpty()) {
            boolean allTokensContained = tokens.stream().allMatch(itemName::contains);
            if (allTokensContained) {
                return 0.88;
            }

            boolean anyWordPrefix = Arrays.stream(itemName.split("\\s+"))
                    .anyMatch(word -> tokens.stream().anyMatch(word::startsWith));
            if (anyWordPrefix) {
                return 0.78;
            }

            double avgSimilarity = tokens.stream()
                    .mapToDouble(token -> bestTokenSimilarity(token, itemName))
                    .average()
                    .orElse(0.0);

            if (avgSimilarity >= SEARCH_FALLBACK_THRESHOLD) {
                return avgSimilarity;
            }
        }

        return 0.0;
    }

    private double bestTokenSimilarity(String token, String normalizedItemName) {
        double best = similarity(token, normalizedItemName);

        for (String word : normalizedItemName.split("\\s+")) {
            best = Math.max(best, similarity(token, word));
        }

        return best;
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

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

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

    /**
     * Enriches search results with bookmark information for the current user.
     *
     * Why:
     * - avoids one DB call per item
     * - fetches all bookmarks in one query
     * - keeps latency low and scalable
     */
    private List<ItemSearchResponse> attachBookmarkStatus(List<ItemSearchResponse> results, String userId) {

        // If user is not logged in or result list is empty, default bookmark status to false
        if (userId == null || userId.isBlank() || results == null || results.isEmpty()) {
            return results.stream()
                    .map(result -> {
                        result.setIsBookmarked(false);
                        return result;
                    })
                    .toList();
        }

        // Collect all item IDs from search results
        List<String> itemIds = results.stream()
                .map(ItemSearchResponse::getItemId)
                .toList();

        // Fetch all matching bookmarked items in one DB query
        List<UserBucketList> bookmarkedEntries =
                userBucketListRepository.findByUserIdAndItemIdInAndIsActiveTrue(userId, itemIds);

        // Convert bookmarked item IDs into a set for O(1) lookup
        java.util.Set<String> bookmarkedItemIds = bookmarkedEntries.stream()
                .map(UserBucketList::getItemId)
                .collect(java.util.stream.Collectors.toSet());

        // Mark each result with bookmark status
        return results.stream()
                .map(result -> {
                    result.setIsBookmarked(bookmarkedItemIds.contains(result.getItemId()));
                    return result;
                })
                .toList();
    }
}