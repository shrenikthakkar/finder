package com.finder.letscheck.dto;

import com.finder.letscheck.model.enums.SearchSource;
import lombok.Builder;
import lombok.Data;

/**
 * Internal DTO used to log search analytics asynchronously.
 */
@Data
@Builder
public class SearchAnalyticsRequest {

    private String query;
    private String normalizedQuery;

    private String userId;

    private String city;
    private String areaName;

    private Double latitude;
    private Double longitude;

    private SearchSource source;

    private Integer resultCount;
}