package com.github.dimitryivaniuta.searchanalytics.web;

import com.github.dimitryivaniuta.searchanalytics.config.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Lightweight HTTP health endpoints, complementing Spring Boot Actuator.
 *
 * Use cases:
 *  - Simple liveness/readiness checks for ingress / load balancers.
 *  - Quick way to see service / topic wiring without hitting /actuator.
 *
 * NOTE: Deep health (DB, Kafka) should still be exposed via /actuator/health.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final KafkaTopicsProperties kafkaTopicsProperties;

    /**
     * Liveness probe – returns static UP status if JVM + web layer are alive.
     */
    @GetMapping("/live")
    public Map<String, Object> live() {
        return Map.of(
                "status", "UP",
                "timestamp", OffsetDateTime.now()
        );
    }

    /**
     * Readiness probe – cheap readiness info:
     *  - exposes configured Kafka topics
     *  - can be extended later to check custom conditions
     */
    @GetMapping("/ready")
    public Map<String, Object> ready() {
        return Map.of(
                "status", "UP",
                "timestamp", OffsetDateTime.now(),
                "kafkaTopics", Map.of(
                        "searchEvents", kafkaTopicsProperties.getSearchEventsTopic(),
                        "searchEventsDlt", kafkaTopicsProperties.getSearchEventsDltTopic(),
                        "outbox", kafkaTopicsProperties.getOutboxTopic()
                )
        );
    }

    /**
     * Simple "ping" endpoint: /api/health
     */
    @GetMapping
    public Map<String, Object> root() {
        return live();
    }
}
