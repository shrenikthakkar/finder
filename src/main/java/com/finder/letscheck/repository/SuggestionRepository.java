package com.finder.letscheck.repository;

import com.finder.letscheck.model.Suggestion;
import com.finder.letscheck.model.enums.SuggestionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SuggestionRepository extends MongoRepository<Suggestion, String> {

    List<Suggestion> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Suggestion> findByStatusOrderByCreatedAtAsc(SuggestionStatus status);
}