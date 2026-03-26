package com.finder.letscheck.search.service;

import com.finder.letscheck.model.Item;
import com.finder.letscheck.repository.ItemRepository;
import com.finder.letscheck.search.dto.ItemSearchRequest;
import com.finder.letscheck.search.dto.ItemSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final MongoTemplate mongoTemplate;
    private final ItemRepository itemRepository;

    public List<ItemSearchResponse> searchNearbyItems(ItemSearchRequest request) {
        validateNearbySearchRequest(request);

        Point userLocation = new Point(request.getLongitude(), request.getLatitude());
        Distance radius = new Distance(request.getRadiusInKm(), Metrics.KILOMETERS);

        Query query = new Query();
        query.addCriteria(
                Criteria.where("location").nearSphere(userLocation).maxDistance(radius.getNormalizedValue())
        );
        query.addCriteria(Criteria.where("isActive").is(true));

        List<Item> items = mongoTemplate.find(query, Item.class);

        return items.stream()
                .map(item -> mapToSearchResponse(item, request.getLatitude(), request.getLongitude()))
                .sorted(defaultSearchComparator())
                .toList();
    }

    public List<ItemSearchResponse> searchNearbyItemsByQuery(ItemSearchRequest request) {
        validateNearbySearchRequest(request);

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new RuntimeException("Query is required for nearby query search");
        }

        String normalizedQuery = normalizeText(request.getQuery());

        Point userLocation = new Point(request.getLongitude(), request.getLatitude());
        Distance radius = new Distance(request.getRadiusInKm(), Metrics.KILOMETERS);

        Query query = new Query();
        query.addCriteria(
                Criteria.where("location").nearSphere(userLocation).maxDistance(radius.getNormalizedValue())
        );
        query.addCriteria(Criteria.where("normalizedItemName").is(normalizedQuery));
        query.addCriteria(Criteria.where("isActive").is(true));

        List<Item> items = mongoTemplate.find(query, Item.class);

        return items.stream()
                .map(item -> mapToSearchResponse(item, request.getLatitude(), request.getLongitude()))
                .sorted(defaultSearchComparator())
                .toList();
    }

    public List<ItemSearchResponse> searchItemsByArea(ItemSearchRequest request) {
        if (request.getCity() == null || request.getCity().isBlank()) {
            throw new RuntimeException("City is required for area search");
        }
        if (request.getArea() == null || request.getArea().isBlank()) {
            throw new RuntimeException("Area is required for area search");
        }

        String normalizedCity = normalizeText(request.getCity());
        String normalizedArea = normalizeText(request.getArea());

        List<Item> items;

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            String normalizedQuery = normalizeText(request.getQuery());
            items = itemRepository.findByNormalizedCityAndNormalizedAreaNameAndNormalizedItemName(
                    normalizedCity,
                    normalizedArea,
                    normalizedQuery
            );
        } else {
            items = itemRepository.findByNormalizedCityAndNormalizedAreaName(
                    normalizedCity,
                    normalizedArea
            );
        }

        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
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
}