package com.github.dimitryivaniuta.searchanalytics.web;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import com.github.dimitryivaniuta.searchanalytics.service.SearchEventProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * REST API for sending search events into Kafka from Postman / external clients.
 */
@RestController
@RequestMapping("/api/search-events")
@RequiredArgsConstructor
public class SearchEventController {

    private final SearchEventProducerService producerService;

    /**
     * POST /api/search-events
     *
     * Accepts SearchEventPayload JSON, enriches it (eventId, occurredAt, sentAt)
     * and publishes to Kafka topic "search-events".
     *
     * Returns basic info so you can see eventId in Postman.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> publish(@RequestBody SearchEventPayload request) {
        SearchEventPayload sent = producerService.sendFromApi(request);
        return Map.of(
                "status", "ACCEPTED",
                "eventId", sent.getEventId(),
                "occurredAt", sent.getOccurredAt(),
                "sentAt", sent.getSentAt(),
                "timestamp", OffsetDateTime.now()
        );
    }
}
