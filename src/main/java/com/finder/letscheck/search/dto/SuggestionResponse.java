package com.finder.letscheck.search.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Represents one autocomplete suggestion shown to the user.
 *
 * Why structured response instead of plain string:
 * - frontend can display richer UI
 * - backend can distinguish suggestion types
 * - future-ready for area/city/item/item-place handling
 */
@Data
@Builder
public class SuggestionResponse {

    /**
     * Suggestion type:
     * ITEM, ALIAS, AREA, CITY
     */
    private String type;

    /**
     * Main text shown in UI.
     * Example: "Pani Puri"
     */
    private String displayText;

    /**
     * Optional secondary text shown in UI.
     * Example: "Pani Puri" for alias "Golgappa"
     */
    private String secondaryText;

    /**
     * Canonical value to be used internally for search.
     * Example:
     * - alias "golgappa" -> canonicalValue = "pani puri"
     * - item "pani puri" -> canonicalValue = "pani puri"
     */
    private String canonicalValue;

    /**
     * Optional location context if suggestion represents area/city.
     */
    private String area;
    private String city;

    /**
     * Score used to rank suggestions.
     * Higher score = more relevant suggestion.
     */
    private Integer score;
}