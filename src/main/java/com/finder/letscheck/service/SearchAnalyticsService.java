package com.finder.letscheck.service;

import com.finder.letscheck.dto.SearchAnalyticsRequest;
import com.finder.letscheck.model.DailySearchStat;
import com.finder.letscheck.model.SearchEvent;
import com.finder.letscheck.repository.DailySearchStatRepository;
import com.finder.letscheck.repository.SearchEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Handles background search analytics logging.
 *
 * Design:
 * - async so user-facing search stays fast
 * - raw event + aggregated daily stat are both updated
 * - best-effort logging is acceptable for analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchEventRepository searchEventRepository;
    private final DailySearchStatRepository dailySearchStatRepository;

    /**
     * Logs search analytics in background.
     *
     * Flow:
     * 1. save raw search event
     * 2. upsert daily aggregate bucket
     */
    @Async
    public void logSearchAsync(SearchAnalyticsRequest request) {
        try {
            saveRawEvent(request);
            updateDailyAggregate(request);
        } catch (Exception ex) {
            log.error("Failed to log search analytics for query={}", request.getQuery(), ex);
        }
    }

    /**
     * Saves one raw search event document.
     */
    private void saveRawEvent(SearchAnalyticsRequest request) {
        SearchEvent event = SearchEvent.builder()
                .query(request.getQuery())
                .normalizedQuery(request.getNormalizedQuery())
                .userId(request.getUserId())
                .city(request.getCity())
                .areaName(request.getAreaName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .source(request.getSource() != null ? request.getSource().name() : null)
                .resultCount(safeInt(request.getResultCount()))
                .zeroResult(safeInt(request.getResultCount()) == 0)
                .searchedAt(Instant.now())
                .build();

        searchEventRepository.save(event);
    }

    /**
     * Updates one daily aggregate bucket.
     *
     * Bucket key:
     * - date
     * - normalized query
     * - city
     * - area
     */
    private void updateDailyAggregate(SearchAnalyticsRequest request) {
        String date = LocalDate.now().toString();
        String normalizedQuery = normalize(
                request.getNormalizedQuery() != null ? request.getNormalizedQuery() : request.getQuery()
        );
        String city = normalizeNullable(request.getCity());
        String areaName = normalizeNullable(request.getAreaName());

        Optional<DailySearchStat> existingOptional =
                dailySearchStatRepository.findByDateAndNormalizedQueryAndCityAndAreaName(
                        date,
                        normalizedQuery,
                        city,
                        areaName
                );

        DailySearchStat stat = existingOptional.orElseGet(() ->
                DailySearchStat.builder()
                        .date(date)
                        .query(request.getQuery())
                        .normalizedQuery(normalizedQuery)
                        .city(city)
                        .areaName(areaName)
                        .searchCount(0)
                        .zeroResultCount(0)
                        .lastSearchedAt(null)
                        .build()
        );

        stat.setSearchCount(safeInt(stat.getSearchCount()) + 1);

        if (safeInt(request.getResultCount()) == 0) {
            stat.setZeroResultCount(safeInt(stat.getZeroResultCount()) + 1);
        }

        stat.setLastSearchedAt(Instant.now().toString());
        dailySearchStatRepository.save(stat);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalize(value);
    }
}