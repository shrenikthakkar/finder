package com.finder.letscheck.repository;

import com.finder.letscheck.model.SearchEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchEventRepository extends MongoRepository<SearchEvent, String> {
}