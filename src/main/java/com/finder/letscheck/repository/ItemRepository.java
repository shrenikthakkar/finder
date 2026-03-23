package com.finder.letscheck.repository;

import com.finder.letscheck.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends MongoRepository<Item, String> {

    Optional<Item> findByRestaurantIdAndNormalizedItemName(String restaurantId, String normalizedItemName);

    List<Item> findByRestaurantId(String restaurantId);
}