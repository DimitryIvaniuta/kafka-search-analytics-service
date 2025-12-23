package com.github.dimitryivaniuta.searchanalytics.jpa.repository;

import com.github.dimitryivaniuta.searchanalytics.jpa.entity.DailyQueryStatEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for daily_query_stats.
 */
public interface DailyQueryStatJpaRepository extends JpaRepository<DailyQueryStatEntity, Long> {

    Optional<DailyQueryStatEntity> findByDayAndQuery(LocalDate day, String query);

    /**
     * "Top N for a day" using Pageable for limit.
     */
    List<DailyQueryStatEntity> findByDayOrderByCountDescQueryAsc(LocalDate day, Pageable pageable);
}
