package com.finder.letscheck.repository;

import com.finder.letscheck.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    Optional<Restaurant> findByNormalizedNameAndNormalizedCityAndNormalizedAreaName(
            String normalizedName,
            String normalizedCity,
            String normalizedAreaName
    );
}