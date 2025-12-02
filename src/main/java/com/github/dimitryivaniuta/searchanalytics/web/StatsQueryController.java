package com.github.dimitryivaniuta.searchanalytics.web;

import com.github.dimitryivaniuta.searchanalytics.service.DailyQueryStatService;
import com.github.dimitryivaniuta.searchanalytics.web.dto.DailyQueryStatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only API for aggregated search statistics.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsQueryController {

    private final DailyQueryStatService dailyQueryStatService;

    @GetMapping("/daily")
    public List<DailyQueryStatResponse> getDailyStats(
            @RequestParam("day")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate day,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return dailyQueryStatService.getTopForDay(day, limit)
                .stream()
                .map(DailyQueryStatResponse::fromModel)
                .toList();
    }

    @GetMapping("/range")
    public List<DailyQueryStatResponse> getRangeStats(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return dailyQueryStatService.getTopInRange(from, to, limit)
                .stream()
                .map(DailyQueryStatResponse::fromModel)
                .toList();
    }
}
