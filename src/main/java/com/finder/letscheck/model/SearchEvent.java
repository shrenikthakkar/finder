package com.finder.letscheck.model;

import com.finder.letscheck.model.enums.SearchSource;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Raw analytics event for a single search execution.
 *
 * Purpose:
 * - debugging
 * - recent behavior analysis
 * - zero-result analysis
 * - future ranking/ML improvements
 *
 * Note:
 * - keep this document lean
 * - use TTL later if needed for auto-expiry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "search_events")
public class SearchEvent {

    @Id
    private String id;

    /**
     * Original and normalized query.
     */
    private String query;
    private String normalizedQuery;

    /**
     * Optional user context.
     */
    private String userId;

    /**
     * Optional location context from parsed request.
     */
    private String city;
    private String areaName;

    private Double latitude;
    private Double longitude;

    /**
     * Search source/type.
     */
    private SearchSource source;

    /**
     * Result summary.
     */
    private Integer resultCount;
    private Boolean zeroResult;

    /**
     * Audit timestamp.
     */
    @Indexed
    private String searchedAt;
}