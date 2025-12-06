package com.github.dimitryivaniuta.searchanalytics.infra;

import com.github.dimitryivaniuta.searchanalytics.SearchAnalyticsApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests.
 *
 * - Starts shared Postgres + Kafka Testcontainers once per JVM.
 * - Runs Flyway migrations explicitly against the Postgres container.
 * - Registers dynamic Spring properties (datasource + Kafka + topics).
 */
@SpringBootTest(
        classes = SearchAnalyticsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                    .withDatabaseName("search_analytics")
                    .withUsername("search_analytics")
                    .withPassword("search_analytics");

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka-native:3.8.0");

    static {
        // 1) Start infra containers
        POSTGRES.start();
        KAFKA.start();

        // 2) Run Flyway migrations manually on the container DB
        //    This guarantees that daily_query_stats, raw_search_events, etc. exist
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Datasource -> Postgres container
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Kafka -> Kafka container
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "search-analytics-cg-test");

        // app topics (override if needed)
        registry.add("app.kafka.search-events-topic", () -> "search-events");
        registry.add("app.kafka.search-events-dlt-topic", () -> "search-events-dlt");
        registry.add("app.kafka.outbox-topic", () -> "search-events-outbox");
    }
}
