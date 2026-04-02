package com.finder.letscheck.repository;

import com.finder.letscheck.model.DailySearchStat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DailySearchStatRepository extends MongoRepository<DailySearchStat, String> {

    Optional<DailySearchStat> findByDateAndNormalizedQueryAndCityAndAreaName(
            String date,
            String normalizedQuery,
            String city,
            String areaName
    );
}