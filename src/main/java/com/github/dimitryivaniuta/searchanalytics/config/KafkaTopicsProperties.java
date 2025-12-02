package com.github.dimitryivaniuta.searchanalytics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds Kafka topic names from application.yml / .env.
 *
 * app.kafka.search-events-topic       -> "search-events"
 * app.kafka.search-events-dlt-topic   -> "search-events-dlt"
 * app.kafka.outbox-topic              -> "search-events-outbox" (optional, for outbox publisher)
 */
@Data
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicsProperties {

    /**
     * Main topic with search events produced by other services / frontend BFF.
     */
    private String searchEventsTopic;

    /**
     * Dead-letter topic for events that could not be processed correctly.
     */
    private String searchEventsDltTopic;

    /**
     * Optional topic for publishing aggregated or outbox events.
     */
    private String outboxTopic;
}
