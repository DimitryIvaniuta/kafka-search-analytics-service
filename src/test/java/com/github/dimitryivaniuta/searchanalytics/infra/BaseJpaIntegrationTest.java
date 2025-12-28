package com.github.dimitryivaniuta.searchanalytics.infra;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for JPA slice tests:
 * - @DataJpaTest (JPA slice)
 * - real Postgres via Testcontainers
 * - Flyway migrations applied
 *
 * IMPORTANT:
 * We start the container explicitly to avoid:
 * "Mapped port can only be obtained after the container is started"
 * during Spring's early DataSource condition evaluation.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public abstract class BaseJpaIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("search_analytics_test")
                .withUsername("search_analytics")
                .withPassword("search_analytics");

        // critical: start before Spring reads properties
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // JDBC
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // JPA – validate schema from Flyway; do NOT auto-create
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        // Hibernate should not create schema (Flyway owns it)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
    }
}
