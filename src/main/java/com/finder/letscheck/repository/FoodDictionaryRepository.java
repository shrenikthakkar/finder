package com.finder.letscheck.repository;

import com.finder.letscheck.model.FoodDictionary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FoodDictionaryRepository extends MongoRepository<FoodDictionary, String> {

    List<FoodDictionary> findByIsActiveTrue();
}