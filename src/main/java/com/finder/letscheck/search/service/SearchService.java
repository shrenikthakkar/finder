package com.finder.letscheck.search.service;

import com.finder.letscheck.model.Item;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.search.dto.ItemSearchRequest;
import com.finder.letscheck.search.dto.ItemSearchResponse;
import com.finder.letscheck.search.parser.QueryParserService;
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


import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final QueryParserService queryParserService;
    private final MongoTemplate mongoTemplate;
    private final ItemRepository itemRepository;

    /**
     * Searches nearby active items using MongoDB geo filtering.
     *
     * Improvement:
     * - Uses geo index for fast nearby search
     * - Applies limit to reduce unnecessary processing
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

        // Only active items should be considered
        query.addCriteria(Criteria.where("isActive").is(true));

        // Fetch only a controlled number of records
        query.limit(limit);

        List<Item> items = mongoTemplate.find(query, Item.class);

        return items.stream()
                .map(item -> mapToSearchResponse(item, request.getLatitude(), request.getLongitude()))
                .sorted(defaultSearchComparator())
                .toList();
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

        String normalizedQuery = normalizeText(request.getQuery());

        Point userLocation = new Point(request.getLongitude(), request.getLatitude());
        Distance radius = new Distance(request.getRadiusInKm(), Metrics.KILOMETERS);

        Query query = new Query();

        // Geo filter for nearby results
        query.addCriteria(
                Criteria.where("location").nearSphere(userLocation).maxDistance(radius.getNormalizedValue())
        );

        // Exact normalized item filter
        query.addCriteria(Criteria.where("normalizedItemName").is(normalizedQuery));

        // Only active items should be returned
        query.addCriteria(Criteria.where("isActive").is(true));

        // Restrict result set size
        query.limit(limit);

        List<Item> items = mongoTemplate.find(query, Item.class);

        return items.stream()
                .map(item -> mapToSearchResponse(item, request.getLatitude(), request.getLongitude()))
                .sorted(defaultSearchComparator())
                .toList();
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

        // At least one location filter must be present
        if ((request.getCity() == null || request.getCity().isBlank())
                && (request.getArea() == null || request.getArea().isBlank())) {
            throw new RuntimeException("Either city or area is required for area search");
        }

        int limit = getSafeLimit(request.getLimit());

        // Normalize request fields for exact matching with normalized DB fields
        String normalizedCity = request.getCity() != null ? normalizeText(request.getCity()) : null;
        String normalizedArea = request.getArea() != null ? normalizeText(request.getArea()) : null;
        String normalizedQuery = request.getQuery() != null ? normalizeText(request.getQuery()) : null;

        Query mongoQuery = new Query();

        // Only active items should be returned
        mongoQuery.addCriteria(Criteria.where("isActive").is(true));

        // Add city filter if available
        if (normalizedCity != null) {
            mongoQuery.addCriteria(Criteria.where("normalizedCity").is(normalizedCity));
        }

        // Add area filter if available
        if (normalizedArea != null) {
            mongoQuery.addCriteria(Criteria.where("normalizedAreaName").is(normalizedArea));
        }

        // Add item filter if available
        if (normalizedQuery != null) {
            mongoQuery.addCriteria(Criteria.where("normalizedItemName").is(normalizedQuery));
        }

        // Restrict result count early
        mongoQuery.limit(limit);

        List<Item> items = mongoTemplate.find(mongoQuery, Item.class);

        return items.stream()
                .map(item -> mapToSearchResponse(item, null, null))
                .sorted(defaultSearchComparator())
                .toList();
    }

    private ItemSearchResponse mapToSearchResponse(Item item, Double userLat, Double userLng) {
        Double distanceInKm = null;

        if (userLat != null && userLng != null && item.getLocation() != null && item.getLocation().getCoordinates() != null
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
    public List<ItemSearchResponse> smartSearch(String rawQuery, Double latitude, Double longitude, Double radiusInKm) {
        QueryParseResult parsed = queryParserService.parse(rawQuery);

        System.out.println("Parsed query => item: " + parsed.getCanonicalItem()
                + ", city: " + parsed.getCity()
                + ", area: " + parsed.getArea()
                + ", nearMe: " + parsed.isNearMeIntent());

        // If area or city is parsed, use area-based search
        if (parsed.getArea() != null || parsed.getCity() != null) {
            ItemSearchRequest request = new ItemSearchRequest();
            request.setCity(parsed.getCity());
            request.setArea(parsed.getArea());
            request.setQuery(parsed.getCanonicalItem());
            request.setLimit(20);
            return searchItemsByArea(request);
        }

        // If query indicates nearby intent or coordinates are available, use nearby search
        if (parsed.isNearMeIntent() || (latitude != null && longitude != null)) {
            ItemSearchRequest request = new ItemSearchRequest();
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setRadiusInKm(radiusInKm != null ? radiusInKm : 5.0);
            request.setQuery(parsed.getCanonicalItem());
            request.setLimit(20);

            if (parsed.getCanonicalItem() != null) {
                return searchNearbyItemsByQuery(request);
            }

            return searchNearbyItems(request);
        }

        // Fallback: item-only search without location
        if (parsed.getCanonicalItem() != null) {
            Query mongoQuery = new Query();
            mongoQuery.addCriteria(Criteria.where("normalizedItemName").is(parsed.getCanonicalItem()));
            mongoQuery.addCriteria(Criteria.where("isActive").is(true));
            mongoQuery.limit(20);

            List<Item> items = mongoTemplate.find(mongoQuery, Item.class);

            return items.stream()
                    .map(item -> mapToSearchResponse(item, latitude, longitude))
                    .sorted(defaultSearchComparator())
                    .toList();
        }

        return List.of();
    }

    /**
     * Returns a safe result limit.
     *
     * Why:
     * - Prevents huge result sets from hurting performance
     * - Gives a default when limit is not provided
     * - Caps maximum results for stability
     */
    private int getSafeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return 20;
        }
        return Math.min(requestedLimit, 50);
    }
}