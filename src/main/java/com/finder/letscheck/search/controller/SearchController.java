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

    @GetMapping("/items/nearby")
    public List<ItemSearchResponse> searchNearbyItems(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusInKm
    ) {
        ItemSearchRequest request = new ItemSearchRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInKm(radiusInKm);

        return searchService.searchNearbyItems(request);
    }

    @GetMapping("/items/nearby/query")
    public List<ItemSearchResponse> searchNearbyItemsByQuery(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusInKm,
            @RequestParam String query
    ) {
        ItemSearchRequest request = new ItemSearchRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInKm(radiusInKm);
        request.setQuery(query);

        return searchService.searchNearbyItemsByQuery(request);
    }

    @GetMapping("/items/by-area")
    public List<ItemSearchResponse> searchItemsByArea(
            @RequestParam String city,
            @RequestParam String area,
            @RequestParam(required = false) String query
    ) {
        ItemSearchRequest request = new ItemSearchRequest();
        request.setCity(city);
        request.setArea(area);
        request.setQuery(query);

        return searchService.searchItemsByArea(request);
    }
}