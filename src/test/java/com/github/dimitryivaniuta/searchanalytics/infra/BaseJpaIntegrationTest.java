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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for JPA slice tests:
 * - Boots only JPA slice (@DataJpaTest)
 * - Uses real Postgres via Testcontainers
 * - Runs Flyway migrations for schema (optional but recommended)
 *
 * NOTE:
 *  - @DataJpaTest by default replaces datasource with embedded DB, so we disable that.
 *  - Flyway is not guaranteed to be included in slice tests, so we import FlywayAutoConfiguration.
 */
@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public abstract class BaseJpaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("search_analytics_test")
                    .withUsername("search_analytics")
                    .withPassword("search_analytics");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // JDBC
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // JPA – validate schema from Flyway; do NOT auto-create
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");

        // Flyway – make sure migrations run for the slice test
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        // Optional:
        // registry.add("spring.flyway.clean-disabled", () -> "true");
    }
}
