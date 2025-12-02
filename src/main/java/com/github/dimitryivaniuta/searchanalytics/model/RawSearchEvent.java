package com.github.dimitryivaniuta.searchanalytics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RawSearchEvent {

    private Long id;

    private String eventKey;
    private String userId;
    private String query;
    private String country;

    private Instant occurredAt;
    private Instant receivedAt;

    private String kafkaTopic;
    private int kafkaPartition;
    private long kafkaOffset;

    /**
     * Original JSON payload as string – stored in JSONB column.
     */
    private String payload;

    /**
     * RECEIVED | PROCESSED | SKIPPED | ERROR
     */
    private String processingStatus;

    private String errorMessage;
}
