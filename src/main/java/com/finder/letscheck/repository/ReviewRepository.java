package com.finder.letscheck.repository;

import com.finder.letscheck.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {

    Optional<Review> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);

    List<Review> findByTargetTypeAndTargetId(String targetType, String targetId);

    List<Review> findByUserId(String userId);
}