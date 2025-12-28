package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.infra.BaseJdbcIntegrationTest;
import com.github.dimitryivaniuta.searchanalytics.model.DailyQueryStat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that daily_query_stats DDL + repository logic work end-to-end.
 */
@Import(DailyQueryStatRepository.class)
class DailyQueryStatJdbcRepositoryIT extends BaseJdbcIntegrationTest {

    @Autowired
    private DailyQueryStatRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // isolate tests: avoid cross-test data pollution (counts/order changes)
        jdbcTemplate.execute("TRUNCATE TABLE daily_query_stats RESTART IDENTITY");
    }

    @Test
    void incrementCount_upsertsAndAccumulates() {
        LocalDate day = LocalDate.of(2025, 12, 7);

        // act – increment same query multiple times
        repository.incrementCount(day, "java");
        repository.incrementCount(day, "java");
        repository.incrementCount(day, "spring");
        repository.incrementCount(day, "java");

        // assert – find by day + query
        DailyQueryStat javaStat = repository.findByDayAndQuery(day, "java").orElseThrow();
        DailyQueryStat springStat = repository.findByDayAndQuery(day, "spring").orElseThrow();

        assertThat(javaStat.getDay()).isEqualTo(day);
        assertThat(javaStat.getQuery()).isEqualTo("java");
        assertThat(javaStat.getCount()).isEqualTo(3);
        assertThat(springStat.getCount()).isEqualTo(1);

        // assert – top for day
        List<DailyQueryStat> top = repository.findTopByDay(day, 10);
        assertThat(top).hasSize(2);
        assertThat(top.get(0).getQuery()).isEqualTo("java");
        assertThat(top.get(0).getCount()).isEqualTo(3);
        assertThat(top.get(1).getQuery()).isEqualTo("spring");
        assertThat(top.get(1).getCount()).isEqualTo(1);
    }

    @Test
    void findTopInRange_aggregatesAcrossDays_andReturnsNullIdAndDay() {
        LocalDate day1 = LocalDate.of(2025, 12, 6);
        LocalDate day2 = LocalDate.of(2025, 12, 7);

        repository.incrementCount(day1, "kafka");
        repository.incrementCount(day1, "kafka");
        repository.incrementCount(day2, "kafka");
        repository.incrementCount(day2, "spring");
        repository.incrementCount(day2, "spring");

        List<DailyQueryStat> agg = repository.findTopInRange(day1, day2, 10);

        // kafka total = 3, spring total = 2
        assertThat(agg).hasSize(2);
        assertThat(agg.get(0).getQuery()).isEqualTo("kafka");
        assertThat(agg.get(0).getCount()).isEqualTo(3);
        assertThat(agg.get(1).getQuery()).isEqualTo("spring");
        assertThat(agg.get(1).getCount()).isEqualTo(2);

        // aggregated rows per your repository contract
        assertThat(agg.get(0).getId()).isNull();
        assertThat(agg.get(0).getDay()).isNull();
    }
}
