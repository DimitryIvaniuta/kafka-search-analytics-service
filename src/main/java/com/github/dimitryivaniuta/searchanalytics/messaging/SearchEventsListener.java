package com.github.dimitryivaniuta.searchanalytics.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import com.github.dimitryivaniuta.searchanalytics.service.DailyQueryStatService;
import com.github.dimitryivaniuta.searchanalytics.service.EventProcessingErrorService;
import com.github.dimitryivaniuta.searchanalytics.service.RawSearchEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Main Kafka listener for search events.
 *
 * IMPORTANT:
 *  - This listener belongs to a single consumer group (configured via spring.kafka.consumer.group-id).
 *  - Listener container factory has concurrency=1.
 *  - Therefore, ONE consumer instance in the group will get ALL partitions for the topic,
 *    exactly as required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchEventsListener {

    private final RawSearchEventService rawSearchEventService;
    private final DailyQueryStatService dailyQueryStatService;
    private final EventProcessingErrorService errorService;
    private final DeadLetterProducer deadLetterProducer;
    private final ObjectMapper objectMapper;

    /**
     * Consumes SearchEventPayload messages from the main topic.
     *
     * We:
     *  1. Log the raw event into raw_search_events.
     *  2. If payload valid -> update daily_query_stats.
     *  3. On success -> mark raw event PROCESSED & ack offset.
     *  4. On any error:
     *      - mark raw event ERROR
     *      - log error into search_event_processing_errors
     *      - send compact message to DLT
     *      - ack offset (so we don't loop forever on poison messages)
     */
    @KafkaListener(
            topics = "${app.kafka.search-events-topic}"
//            containerFactory = "searchEventsKafkaListenerContainerFactory"
    )
    public void onMessage(SearchEventPayload payload,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          ConsumerRecord<String, SearchEventPayload> record,
                          Acknowledgment ack) {

        Long rawEventId = null;

        try {
            // 1) Store raw event (with original JSON from record.value())
            // record.value() is already SearchEventPayload; if you want raw JSON, serialize again.
            String rawJson = safeToJson(record.value());

            rawEventId = rawSearchEventService.logReceivedEvent(
                    key,
                    topic,
                    partition,
                    offset,
                    payload,
                    rawJson
            );

            if (rawEventId != null) {
                rawSearchEventService.markProcessed(rawEventId);
            }

            // 2) Validate payload before aggregation
            if (!payload.isValidForAggregation()) {
                String msg = "SearchEventPayload invalid for aggregation (missing query or occurredAt)";
                log.warn("{}; key={}, topic={}, partition={}, offset={}",
                        msg, key, topic, partition, offset);

                rawSearchEventService.markError(rawEventId, msg);
                errorService.logError(
                        rawEventId,
                        topic,
                        partition,
                        offset,
                        "VALIDATION",
                        msg,
                        null
                );
                // We still ack to move on.
                ack.acknowledge();
                return;
            }

            // 3) Update aggregated statistics table
            dailyQueryStatService.incrementFromEvent(payload);

            // 4) Mark raw event as processed and commit offset
            rawSearchEventService.markProcessed(rawEventId);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process search event; key={}, topic={}, partition={}, offset={}",
                    key, topic, partition, offset, ex);

            if (rawEventId != null) {
                rawSearchEventService.markError(rawEventId, ex.getMessage());
            }

            Long errorId = errorService.logError(
                    rawEventId,
                    topic,
                    partition,
                    offset,
                    "PROCESSING_ERROR",
                    ex.getMessage(),
                    stackTraceAsString(ex)
            );

            log.warn("Recorded processing error with id={}", errorId);

            // Send a compact version of the failing event to DLT
            deadLetterProducer.sendToDlt(key, payload, topic, partition, offset, ex);

            // Ack even on failure to avoid infinite retries on poison messages.
            ack.acknowledge();
        }
    }

    /**
     * Helper: serialize payload back to JSON for storing in raw_search_events.
     * For simplicity we use a very lightweight approach here; in real code you
     * would inject ObjectMapper as a bean.
     */
    private String safeToJson(SearchEventPayload payload) {
        try {
            // Mini inline mapper
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize SearchEventPayload to JSON, storing toString() instead", e);
            return String.valueOf(payload);
        }
    }

    private String stackTraceAsString(Throwable t) {
        if (t == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
