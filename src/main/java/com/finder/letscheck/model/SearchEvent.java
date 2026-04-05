package com.finder.letscheck.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "search_events")
public class SearchEvent {

    @Id
    private String id;

    private String query;
    private String normalizedQuery;
    private String userId;

    private String city;
    private String areaName;

    private Double latitude;
    private Double longitude;

    private Integer resultCount;
    private Boolean zeroResult;
    private String source;

    /**
     * TTL: raw events auto-delete after 15 days.
     */
    @Indexed(expireAfterSeconds = 15 * 24 * 60 * 60)
    private Instant searchedAt;
}