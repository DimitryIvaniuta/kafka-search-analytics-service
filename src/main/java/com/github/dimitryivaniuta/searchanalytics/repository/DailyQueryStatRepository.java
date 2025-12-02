package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.model.DailyQueryStat;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based repository for daily_query_stats table.
 */
@Repository
@RequiredArgsConstructor
public class DailyQueryStatRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<DailyQueryStat> ROW_MAPPER = new DailyQueryStatRowMapper();

    /**
     * Inserts a new row or increments count if (day, query) already exists.
     * Uses the UNIQUE constraint on (day, query).
     */
    public void incrementCount(LocalDate day, String query) {
        String sql = """
            INSERT INTO daily_query_stats(day, query, count)
            VALUES (?, ?, 1)
            ON CONFLICT (day, query)
            DO UPDATE SET count = daily_query_stats.count + 1
            """;
        jdbcTemplate.update(sql, day, query);
    }

    /**
     * Finds a single row by day and query, if it exists.
     */
    public Optional<DailyQueryStat> findByDayAndQuery(LocalDate day, String query) {
        String sql = """
            SELECT id, day, query, count
            FROM daily_query_stats
            WHERE day = ? AND query = ?
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, day, query)
                .stream()
                .findFirst();
    }

    /**
     * Returns top queries for a given day ordered by count desc.
     */
    public List<DailyQueryStat> findTopByDay(LocalDate day, int limit) {
        String sql = """
            SELECT id, day, query, count
            FROM daily_query_stats
            WHERE day = ?
            ORDER BY count DESC, query ASC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, day, limit);
    }

    /**
     * Aggregates across a day range: sums counts per query.
     * Useful for "top queries this week/month".
     */
    public List<DailyQueryStat> findTopInRange(LocalDate from, LocalDate to, int limit) {
        String sql = """
            SELECT
                NULL AS id,
                NULL AS day,
                query,
                SUM(count) AS count
            FROM daily_query_stats
            WHERE day BETWEEN ? AND ?
            GROUP BY query
            ORDER BY SUM(count) DESC, query ASC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> DailyQueryStat.builder()
                .id(null)
                .day(null) // aggregated across days
                .query(rs.getString("query"))
                .count(rs.getLong("count"))
                .build(), from, to, limit);
    }

    /**
     * Simple mapper for daily_query_stats rows.
     */
    private static class DailyQueryStatRowMapper implements RowMapper<DailyQueryStat> {
        @Override
        public DailyQueryStat mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DailyQueryStat.builder()
                    .id(rs.getLong("id"))
                    .day(rs.getObject("day", LocalDate.class))
                    .query(rs.getString("query"))
                    .count(rs.getLong("count"))
                    .build();
        }
    }
}
