package com.finder.letscheck.search.dto;

import lombok.Data;

@Data
public class ItemSearchRequest {

    private Double latitude;
    private Double longitude;
    private Double radiusInKm;

    private String query;

    private String city;
    private String area;

    // Maximum number of results to return
    private Integer limit;
}