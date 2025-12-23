package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.infra.BaseJpaIntegrationTest;
import com.github.dimitryivaniuta.searchanalytics.jpa.entity.DailyQueryStatEntity;
import com.github.dimitryivaniuta.searchanalytics.jpa.repository.DailyQueryStatJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DailyQueryStatJpaRepositoryIT extends BaseJpaIntegrationTest {

    @Autowired
    private DailyQueryStatJpaRepository repository;

    @Test
    void saveAndFindByDayAndQuery() {
        LocalDate day = LocalDate.of(2025, 12, 7);

        DailyQueryStatEntity saved = repository.saveAndFlush(DailyQueryStatEntity.builder()
                .day(day)
                .query("spring kafka")
                .count(5)
                .build());

        assertThat(saved.getId()).isNotNull();

        DailyQueryStatEntity found = repository.findByDayAndQuery(day, "spring kafka").orElseThrow();
        assertThat(found.getCount()).isEqualTo(5);
    }

    @Test
    void uniqueConstraint_dayAndQuery_isEnforced() {
        LocalDate day = LocalDate.of(2025, 12, 7);

        repository.saveAndFlush(DailyQueryStatEntity.builder()
                .day(day)
                .query("java")
                .count(1)
                .build());

        // same (day, query) should violate UNIQUE(day, query)
        assertThatThrownBy(() -> repository.saveAndFlush(DailyQueryStatEntity.builder()
                .day(day)
                .query("java")
                .count(2)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findTopByDay_ordersByCountDescThenQueryAsc() {
        LocalDate day = LocalDate.of(2025, 12, 7);

        repository.saveAll(List.of(
                DailyQueryStatEntity.builder().day(day).query("b").count(5).build(),
                DailyQueryStatEntity.builder().day(day).query("a").count(5).build(),
                DailyQueryStatEntity.builder().day(day).query("c").count(2).build()
        ));
        repository.flush();

        List<DailyQueryStatEntity> top2 =
                repository.findByDayOrderByCountDescQueryAsc(day, PageRequest.of(0, 2));

        assertThat(top2).hasSize(2);
        // count desc, then query asc -> a then b (both count=5)
        assertThat(top2.get(0).getQuery()).isEqualTo("a");
        assertThat(top2.get(1).getQuery()).isEqualTo("b");
    }
}
