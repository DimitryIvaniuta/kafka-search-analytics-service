package com.github.dimitryivaniuta.searchanalytics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SearchEventProcessingError {

    private Long id;

    private Long rawEventId;

    private String kafkaTopic;
    private int kafkaPartition;
    private long kafkaOffset;

    private String errorType;   // VALIDATION, DB_CONSTRAINT, UNKNOWN, etc.
    private String errorMessage;
    private String stackTrace;

    private int retryCount;
    private Instant lastRetryAt;
    private Instant occurredAt;
}
