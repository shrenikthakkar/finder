package com.finder.letscheck.repository;

import com.finder.letscheck.model.RewardTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RewardTransactionRepository extends MongoRepository<RewardTransaction, String> {

    List<RewardTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}