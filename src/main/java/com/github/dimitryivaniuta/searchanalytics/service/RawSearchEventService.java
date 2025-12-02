package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.model.RawSearchEvent;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import com.github.dimitryivaniuta.searchanalytics.repository.RawSearchEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RawSearchEventService {

    private final RawSearchEventRepository repository;

    public Long logReceivedEvent(
            String key,
            String topic,
            int partition,
            long offset,
            SearchEventPayload payload,
            String rawJson
    ) {
        RawSearchEvent event = RawSearchEvent.builder()
                .eventKey(key)
                .userId(payload.getUserId())
                .query(payload.getQuery())
                .country(payload.getCountry())
                .occurredAt(payload.getOccurredAt())
                .receivedAt(Instant.now())
                .kafkaTopic(topic)
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .payload(rawJson)
                .processingStatus("RECEIVED")
                .build();
        return repository.save(event);
    }

    public void markProcessed(Long id) {
        repository.updateStatusAndError(id, "PROCESSED", null);
    }

    public void markError(Long id, String errorMessage) {
        repository.updateStatusAndError(id, "ERROR", errorMessage);
    }
}
