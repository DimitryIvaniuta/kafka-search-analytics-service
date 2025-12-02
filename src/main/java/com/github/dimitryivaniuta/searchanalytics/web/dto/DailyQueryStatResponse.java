package com.github.dimitryivaniuta.searchanalytics.web.dto;

import com.github.dimitryivaniuta.searchanalytics.model.DailyQueryStat;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * API DTO for exposing statistics over HTTP.
 */
@Value
@Builder
public class DailyQueryStatResponse {

    Long id;
    LocalDate day;
    String query;
    long count;

    public static DailyQueryStatResponse fromModel(DailyQueryStat stat) {
        return DailyQueryStatResponse.builder()
                .id(stat.getId())
                .day(stat.getDay())
                .query(stat.getQuery())
                .count(stat.getCount())
                .build();
    }
}
