package com.github.dimitryivaniuta.searchanalytics.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.searchanalytics.config.KafkaTopicsProperties;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Produces messages into the dead-letter topic when processing fails.
 * Value is a JSON containing original payload + basic error context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterProducer {

    private final KafkaTemplate<String, String> jsonStringKafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends an event to DLT. We wrap the original payload and error info into one JSON object.
     */
    public void sendToDlt(String originalKey,
                          SearchEventPayload payload,
                          String kafkaTopic,
                          int kafkaPartition,
                          long kafkaOffset,
                          Throwable error) {

        Map<String, Object> body = new HashMap<>();
        body.put("originalKey", originalKey);
        body.put("originalTopic", kafkaTopic);
        body.put("originalPartition", kafkaPartition);
        body.put("originalOffset", kafkaOffset);
        body.put("errorMessage", error != null ? error.getMessage() : null);
        body.put("errorType", error != null ? error.getClass().getName() : null);
        body.put("payload", payload);

        try {
            String json = objectMapper.writeValueAsString(body);
            jsonStringKafkaTemplate.send(topics.getSearchEventsDltTopic(), originalKey, json);
            log.warn("Sent message to DLT topic='{}', key='{}', offset={}",
                    topics.getSearchEventsDltTopic(), originalKey, kafkaOffset);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DLT payload for topic={}, partition={}, offset={}",
                    kafkaTopic, kafkaPartition, kafkaOffset, e);
        }
    }
}
