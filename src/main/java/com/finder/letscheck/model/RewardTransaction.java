package com.finder.letscheck.model;

import com.finder.letscheck.model.enums.RewardTransactionType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Stores reward point history for users.
 *
 * Why separate collection:
 * - audit trail
 * - avoids double-credit bugs
 * - supports future claims/redeems
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reward_transactions")
public class RewardTransaction {

    @Id
    private String id;

    private String userId;

    private RewardTransactionType type;

    /**
     * Usually suggestionId for suggestion-based rewards.
     */
    private String referenceId;

    /**
     * Positive for credit, negative for debit.
     */
    private Integer points;

    private String description;

    private String createdAt;
}