package com.finder.letscheck.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents application user.
 *
 * NOTE:
 * - Contains profile + summary fields only
 * - No heavy lists (like bookmarks) stored here
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true, sparse = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    private String profileImageUrl;

    private String bio;

    /**
     * Total reviews written by user
     */
    private Integer totalReviewsGiven;

    /**
     * Cities explored by user
     */
    private List<String> citiesVisited;

    private Integer citiesVisitedCount;

    private String role;

    private Boolean isActive;

    private Boolean isProfilePublic;

    private String createdAt;

    private String updatedAt;
}