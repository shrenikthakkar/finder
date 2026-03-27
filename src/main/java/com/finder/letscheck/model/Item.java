package com.finder.letscheck.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Item document stored in MongoDB.
 *
 * Improvements:
 * - Geo index supports nearby search
 * - Single field indexes help exact lookup
 * - Compound indexes help city/area/item search patterns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "items")
@CompoundIndexes({
        @CompoundIndex(name = "city_area_idx", def = "{'normalizedCity': 1, 'normalizedAreaName': 1}"),
        @CompoundIndex(name = "city_area_item_idx", def = "{'normalizedCity': 1, 'normalizedAreaName': 1, 'normalizedItemName': 1}")
})
public class Item {

    @Id
    private String id;

    private String restaurantId;
    private String restaurantName;

    private String itemName;

    @Indexed
    private String normalizedItemName;

    private String category;
    private String subCategory;

    private Double price;
    private String currency;

    private Boolean isVeg;
    private Boolean isAvailable;

    private Double avgItemRating;
    private Integer ratingCount;
    private Integer ratingSum;

    private String areaName;

    @Indexed
    private String normalizedAreaName;

    private String city;

    @Indexed
    private String normalizedCity;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private Location location;

    private String createdBy;
    private String createdByType;

    private String status;
    private Boolean isVerified;
    private Boolean isActive;
}