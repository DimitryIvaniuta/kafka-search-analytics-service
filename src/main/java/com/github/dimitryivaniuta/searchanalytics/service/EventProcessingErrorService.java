package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventProcessingError;
import com.github.dimitryivaniuta.searchanalytics.repository.SearchEventProcessingErrorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventProcessingErrorService {

    private final SearchEventProcessingErrorRepository repository;

    public Long logError(
            Long rawEventId,
            String topic,
            int partition,
            long offset,
            String errorType,
            String errorMessage,
            String stackTrace
    ) {
        SearchEventProcessingError error = SearchEventProcessingError.builder()
                .rawEventId(rawEventId)
                .kafkaTopic(topic)
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .errorType(errorType)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .retryCount(0)
                .occurredAt(Instant.now())
                .build();

        return repository.save(error);
    }

    public void incrementRetry(Long errorId) {
        repository.incrementRetry(errorId);
    }
}
