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
 *
 */

/**
 * Application user.
 *
 * Auth-related fields added:
 * - email
 * - phone
 * - passwordHash
 * - role
 *
 * Business/profile fields can continue to stay in this same document for now.
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

    private Boolean isActive;

    private Boolean isProfilePublic;

    private String createdAt;

    private String updatedAt;

    /**
     * Current reward points balance.
     */
    private Integer rewardPointsBalance;

    /**
     * Total approved contribution count.
     */
    private Integer approvedContributionCount;

    /**
     * Total pending contribution count.
     */
    private Integer pendingContributionCount;

    /**
     * Total rejected contribution count.
     */
    private Integer rejectedContributionCount;

    /**
     * Hashed password using BCrypt.
     * Never store plain password.
     */
    private String passwordHash;

    /**
     * Role used for authorization.
     * Example values: USER, ADMIN
     */
    private String role;

    /**
     * Public display name shown in reviews and other public surfaces.
     * This should not expose the user's real name/email unless they choose so.
     */
    private String publicUsername;

}