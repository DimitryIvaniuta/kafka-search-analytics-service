package com.github.dimitryivaniuta.searchanalytics.infra;

import com.github.dimitryivaniuta.searchanalytics.SearchAnalyticsApplication;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Base class for all integration tests.
 *
 * - Starts shared Postgres + Kafka containers once per test run.
 * - Registers Spring Boot properties via DynamicPropertySource.
 */
@Testcontainers
@SpringBootTest(
        classes = SearchAnalyticsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    // IntelliJ warns about “used without try-with-resources” for static containers.
    // In Testcontainers JUnit pattern this is intentional – Testcontainers/JUnit
    // manage lifecycle. We explicitly suppress the warning.

    @Container
//    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("search_analytics")
                    .withUsername("search_analytics")
                    .withPassword("search_analytics");

    // Use the new Kafka module type instead of deprecated org.testcontainers.containers.KafkaContainer
    @Container
//    @SuppressWarnings("resource")
    protected static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka-native:3.8.0"); // KRaft-based image :contentReference[oaicite:1]{index=1}

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // ---- Datasource ----
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // ---- Kafka ----
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "search-analytics-cg-test");

        // ---- app topics (override if needed) ----
        registry.add("app.kafka.search-events-topic", () -> "search-events");
        registry.add("app.kafka.search-events-dlt-topic", () -> "search-events-dlt");
        registry.add("app.kafka.outbox-topic", () -> "search-events-outbox");
    }
}
