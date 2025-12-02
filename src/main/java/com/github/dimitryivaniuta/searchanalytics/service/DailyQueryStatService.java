package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.model.DailyQueryStat;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import com.github.dimitryivaniuta.searchanalytics.repository.DailyQueryStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Domain service for updating and querying daily search statistics.
 */
@Service
@RequiredArgsConstructor
public class DailyQueryStatService {

    private final DailyQueryStatRepository repository;

    /**
     * Called from Kafka listener (or another orchestration service) to
     * increment daily stats based on incoming search event.
     */
    public void incrementFromEvent(SearchEventPayload event) {
        LocalDate day = event.getOccurredAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        repository.incrementCount(day, event.getQuery());
    }

    public List<DailyQueryStat> getTopForDay(LocalDate day, int limit) {
        return repository.findTopByDay(day, limit);
    }

    public List<DailyQueryStat> getTopInRange(LocalDate from, LocalDate to, int limit) {
        return repository.findTopInRange(from, to, limit);
    }
}
