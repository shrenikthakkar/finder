package com.finder.letscheck.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "food_dictionary")
public class FoodDictionary {

    @Id
    private String id;

    private String canonicalName;

    @Indexed
    private String normalizedCanonicalName;

    private List<FoodAlias> aliases;

    private String category;

    private Boolean isActive;

    private String createdAt;
    private String updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FoodAlias {
        private String name;
        private String normalizedName;
        private List<String> stateScope;
        private Boolean isActive;
    }
}