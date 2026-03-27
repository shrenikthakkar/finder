package com.finder.letscheck.search.controller;

import com.finder.letscheck.search.dto.ItemSearchRequest;
import com.finder.letscheck.search.dto.ItemSearchResponse;
import com.finder.letscheck.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Searches nearby items around a user location.
     */
    @GetMapping("/items/nearby")
    public List<ItemSearchResponse> searchNearbyItems(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusInKm,
            @RequestParam(required = false) Integer limit
    ) {
        ItemSearchRequest request = new ItemSearchRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInKm(radiusInKm);
        request.setLimit(limit);

        return searchService.searchNearbyItems(request);
    }

    /**
     * Searches nearby items for a specific query.
     */
    @GetMapping("/items/nearby/query")
    public List<ItemSearchResponse> searchNearbyItemsByQuery(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusInKm,
            @RequestParam String query,
            @RequestParam(required = false) Integer limit
    ) {
        ItemSearchRequest request = new ItemSearchRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInKm(radiusInKm);
        request.setQuery(query);
        request.setLimit(limit);

        return searchService.searchNearbyItemsByQuery(request);
    }

    /**
     * Searches items by city / area / optional item query.
     */
    @GetMapping("/items/by-area")
    public List<ItemSearchResponse> searchItemsByArea(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit
    ) {
        ItemSearchRequest request = new ItemSearchRequest();
        request.setCity(city);
        request.setArea(area);
        request.setQuery(query);
        request.setLimit(limit);

        return searchService.searchItemsByArea(request);
    }

    /**
     * Smart search endpoint for raw user text.
     */
    @GetMapping("/items/smart")
    public List<ItemSearchResponse> smartSearch(
            @RequestParam String query,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusInKm
    ) {
        return searchService.smartSearch(query, latitude, longitude, radiusInKm);
    }
}