package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.config.KafkaTopicsProperties;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Sends SearchEventPayload messages to Kafka topic configured as search-events.
 * Used by REST API (Postman) and potentially by other internal callers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchEventProducerService {

    private final KafkaTemplate<String, SearchEventPayload> searchEventKafkaTemplate;
    private final KafkaTopicsProperties topics;

    /**
     * Normalizes payload (eventId, occurredAt, sentAt, key) and sends to Kafka.
     *
     * @param incoming payload from API/client
     * @return the normalized payload that was actually sent
     */
    public SearchEventPayload sendFromApi(SearchEventPayload incoming) {
        SearchEventPayload payload = enrichPayload(incoming);

        String key = resolveKey(payload);

        log.info("Producing search event to topic='{}', key='{}', eventId='{}'",
                topics.getSearchEventsTopic(), key, payload.getEventId());

        searchEventKafkaTemplate.send(topics.getSearchEventsTopic(), key, payload);

        return payload;
    }

    private SearchEventPayload enrichPayload(SearchEventPayload payload) {
        // Ensure we can mutate (Lombok @Data gives setters)
        if (payload.getEventId() == null || payload.getEventId().isBlank()) {
            payload.setEventId(UUID.randomUUID().toString());
        }
        Instant now = Instant.now();
        if (payload.getOccurredAt() == null) {
            payload.setOccurredAt(now);
        }
        if (payload.getSentAt() == null) {
            payload.setSentAt(now);
        }
        return payload;
    }

    private String resolveKey(SearchEventPayload payload) {
        if (payload.getUserId() != null && !payload.getUserId().isBlank()) {
            return payload.getUserId();
        }
        if (payload.getAnonymousId() != null && !payload.getAnonymousId().isBlank()) {
            return payload.getAnonymousId();
        }
        if (payload.getSessionId() != null && !payload.getSessionId().isBlank()) {
            return payload.getSessionId();
        }
        // Fallback – stable key based on eventId
        return payload.getEventId();
    }
}
