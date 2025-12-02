package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.model.DailyQueryStat;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventOutbox;
import com.github.dimitryivaniuta.searchanalytics.repository.SearchEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchEventOutboxService {

    private final SearchEventOutboxRepository repository;

    /**
     * Example: create an outbox event when daily stats are updated.
     * In real code, build a proper JSON payload.
     */
    public Long createStatsUpdatedEvent(DailyQueryStat stat) {
        String payloadJson = """
            {
              "day": "%s",
              "query": "%s",
              "count": %d
            }
            """.formatted(stat.getDay(), stat.getQuery(), stat.getCount());

        SearchEventOutbox event = SearchEventOutbox.builder()
                .aggregateType("DailyQueryStat")
                .aggregateId(String.valueOf(stat.getId()))
                .eventType("SEARCH_STATS_UPDATED")
                .payload(payloadJson)
                .headers(null)
                .partitionKey(stat.getQuery())
                .status("NEW")
                .createdAt(Instant.now())
                .build();
        return repository.save(event);
    }

    public List<SearchEventOutbox> findNextNewEvents(int batchSize) {
        return repository.findNextNewEvents(batchSize);
    }

    public void markPublished(Long id) {
        repository.markPublished(id);
    }

    public void markFailed(Long id, String errorMessage) {
        repository.markFailed(id, errorMessage);
    }
}
