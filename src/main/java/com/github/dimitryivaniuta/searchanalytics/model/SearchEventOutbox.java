package com.github.dimitryivaniuta.searchanalytics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SearchEventOutbox {

    private Long id;

    private String aggregateType;
    private String aggregateId;

    private String eventType;
    private String payload;      // JSON body
    private String headers;      // JSON metadata
    private String partitionKey; // key for Kafka partitioning

    /**
     * NEW | PUBLISHED | FAILED
     */
    private String status;

    private Instant createdAt;
    private Instant publishedAt;
    private String lastError;
}
