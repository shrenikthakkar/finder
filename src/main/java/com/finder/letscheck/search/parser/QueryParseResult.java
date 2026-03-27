package com.finder.letscheck.search.parser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryParseResult {

    private String originalQuery;
    private String normalizedQuery;

    private String canonicalItem;
    private String matchedItemToken;

    private String city;
    private String area;

    private boolean nearMeIntent;
    private boolean areaIntent;

    private double itemConfidence;
    private double cityConfidence;
    private double areaConfidence;
}