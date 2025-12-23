package com.github.dimitryivaniuta.searchanalytics.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * JPA mapping for table daily_query_stats.
 *
 * Table constraints expected (Flyway):
 *  - id BIGSERIAL PK
 *  - day DATE NOT NULL
 *  - query TEXT/VARCHAR NOT NULL
 *  - count BIGINT NOT NULL
 *  - UNIQUE(day, query)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "daily_query_stats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_daily_query_stats_day_query", columnNames = {"day", "query"})
        }
)
public class DailyQueryStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    /**
     * "query" is a safe identifier in Postgres, but keep explicit column name.
     */
    @Column(name = "query", nullable = false)
    private String query;

    /**
     * Use primitive long to mirror NOT NULL column.
     */
    @Column(name = "count", nullable = false)
    private long count;
}
