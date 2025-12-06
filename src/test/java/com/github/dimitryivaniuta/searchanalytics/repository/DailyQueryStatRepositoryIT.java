package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.infra.BaseIntegrationTest;
import com.github.dimitryivaniuta.searchanalytics.model.DailyQueryStat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that daily_query_stats DDL + repository logic work end-to-end.
 */
class DailyQueryStatRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private DailyQueryStatRepository repository;

    @Test
    void incrementCount_insertsAndAggregatesCorrectly() {
        LocalDate today = LocalDate.now();

        // act – increment same query multiple times
        repository.incrementCount(today, "java");
        repository.incrementCount(today, "java");
        repository.incrementCount(today, "spring");
        repository.incrementCount(today, "java");

        // assert – find by day + query
        DailyQueryStat javaStat = repository.findByDayAndQuery(today, "java")
                .orElseThrow();
        DailyQueryStat springStat = repository.findByDayAndQuery(today, "spring")
                .orElseThrow();

        assertThat(javaStat.getCount()).isEqualTo(3);
        assertThat(springStat.getCount()).isEqualTo(1);

        // assert – top for day
        List<DailyQueryStat> top = repository.findTopByDay(today, 10);
        assertThat(top)
                .extracting(DailyQueryStat::getQuery)
                .containsExactly("java", "spring");
    }

    @Test
    void findTopInRange_aggregatesAcrossDays() {
        LocalDate day1 = LocalDate.now().minusDays(1);
        LocalDate day2 = LocalDate.now();

        repository.incrementCount(day1, "kafka");
        repository.incrementCount(day1, "kafka");
        repository.incrementCount(day2, "kafka");
        repository.incrementCount(day2, "spring");
        repository.incrementCount(day2, "spring");

        List<DailyQueryStat> top = repository.findTopInRange(day1, day2, 10);

        assertThat(top).isNotEmpty();
        DailyQueryStat kafkaAgg = top.stream()
                .filter(s -> "kafka".equals(s.getQuery()))
                .findFirst()
                .orElseThrow();

        assertThat(kafkaAgg.getCount()).isEqualTo(3);
        // aggregated row has no id/day set
        assertThat(kafkaAgg.getId()).isNull();
        assertThat(kafkaAgg.getDay()).isNull();
    }
}
