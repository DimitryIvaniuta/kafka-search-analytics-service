package com.github.dimitryivaniuta.searchanalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Search Analytics microservice.
 *
 * Responsibilities:
 *  - Bootstraps Spring Boot (auto-config, component scan, Actuator).
 *  - Scans all classes under com.github.dimitryivaniuta.searchanalytics.*.
 */
@SpringBootApplication
public class SearchAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchAnalyticsApplication.class, args);
    }
}
