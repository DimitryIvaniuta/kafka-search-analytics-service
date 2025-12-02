package com.github.dimitryivaniuta.searchanalytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Domain model mapped to daily_query_stats table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyQueryStat {

    private Long id;
    private LocalDate day;
    private String query;
    private long count;
}
