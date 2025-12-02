package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchEventOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public Long save(SearchEventOutbox event) {
        String sql = """
            INSERT INTO search_event_outbox (
                aggregate_type, aggregate_id,
                event_type, payload, headers, partition_key,
                status, created_at, published_at, last_error
            ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, event.getAggregateType());
            ps.setString(2, event.getAggregateId());
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getPayload());
            ps.setString(5, event.getHeaders());
            ps.setString(6, event.getPartitionKey());
            ps.setString(7, event.getStatus());
            ps.setTimestamp(8, toTimestamp(event.getCreatedAt()));
            ps.setTimestamp(9, toTimestamp(event.getPublishedAt()));
            ps.setString(10, event.getLastError());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    /**
     * Finds NEW events ordered by created_at for publishing.
     */
    public List<SearchEventOutbox> findNextNewEvents(int limit) {
        String sql = """
            SELECT id, aggregate_type, aggregate_id,
                   event_type, payload, headers, partition_key,
                   status, created_at, published_at, last_error
            FROM search_event_outbox
            WHERE status = 'NEW'
            ORDER BY created_at ASC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> SearchEventOutbox.builder()
                        .id(rs.getLong("id"))
                        .aggregateType(rs.getString("aggregate_type"))
                        .aggregateId(rs.getString("aggregate_id"))
                        .eventType(rs.getString("event_type"))
                        .payload(rs.getString("payload"))
                        .headers(rs.getString("headers"))
                        .partitionKey(rs.getString("partition_key"))
                        .status(rs.getString("status"))
                        .createdAt(toInstant(rs.getTimestamp("created_at")))
                        .publishedAt(toInstant(rs.getTimestamp("published_at")))
                        .lastError(rs.getString("last_error"))
                        .build(),
                limit
        );
    }

    public void markPublished(Long id) {
        String sql = """
            UPDATE search_event_outbox
            SET status = 'PUBLISHED',
                published_at = NOW(),
                last_error = NULL
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, id);
    }

    public void markFailed(Long id, String errorMessage) {
        String sql = """
            UPDATE search_event_outbox
            SET status = 'FAILED',
                last_error = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, errorMessage, id);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
