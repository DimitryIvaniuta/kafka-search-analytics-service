package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventProcessingError;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class SearchEventProcessingErrorRepository {

    private final JdbcTemplate jdbcTemplate;

    public Long save(SearchEventProcessingError error) {
        String sql = """
            INSERT INTO search_event_processing_errors (
                raw_event_id,
                kafka_topic, kafka_partition, kafka_offset,
                error_type, error_message, stack_trace,
                retry_count, last_retry_at, occurred_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (error.getRawEventId() != null) {
                ps.setLong(1, error.getRawEventId());
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, error.getKafkaTopic());
            ps.setInt(3, error.getKafkaPartition());
            ps.setLong(4, error.getKafkaOffset());
            ps.setString(5, error.getErrorType());
            ps.setString(6, error.getErrorMessage());
            ps.setString(7, error.getStackTrace());
            ps.setInt(8, error.getRetryCount());
            ps.setTimestamp(9, toTimestamp(error.getLastRetryAt()));
            ps.setTimestamp(10, toTimestamp(error.getOccurredAt()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public void incrementRetry(Long id) {
        String sql = """
            UPDATE search_event_processing_errors
            SET retry_count = retry_count + 1,
                last_retry_at = NOW()
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, id);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
