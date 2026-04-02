package com.finder.letscheck.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Aggregated daily search analytics bucket.
 *
 * One document typically represents:
 * - one day
 * - one normalized query
 * - one city
 * - one area (optional)
 *
 * Purpose:
 * - fast reporting
 * - trending queries
 * - missing-query detection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "daily_search_stats")
@CompoundIndex(
        name = "daily_query_city_area_idx",
        def = "{'date': 1, 'normalizedQuery': 1, 'city': 1, 'areaName': 1}",
        unique = true
)
public class DailySearchStat {

    @Id
    private String id;

    /**
     * Date bucket in YYYY-MM-DD format.
     */
    private String date;

    private String query;
    private String normalizedQuery;

    private String city;
    private String areaName;

    /**
     * Aggregated counters.
     */
    private Integer searchCount;
    private Integer zeroResultCount;

    /**
     * Audit timestamps.
     */
    private String lastSearchedAt;
}