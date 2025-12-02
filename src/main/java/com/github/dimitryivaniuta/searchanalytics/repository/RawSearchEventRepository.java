package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.model.RawSearchEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RawSearchEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public Long save(RawSearchEvent event) {
        String sql = """
            INSERT INTO raw_search_events (
                event_key, user_id, query, country,
                occurred_at, received_at,
                kafka_topic, kafka_partition, kafka_offset,
                payload, processing_status, error_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, event.getEventKey());
            ps.setString(2, event.getUserId());
            ps.setString(3, event.getQuery());
            ps.setString(4, event.getCountry());
            ps.setTimestamp(5, toTimestamp(event.getOccurredAt()));
            ps.setTimestamp(6, toTimestamp(event.getReceivedAt()));
            ps.setString(7, event.getKafkaTopic());
            ps.setInt(8, event.getKafkaPartition());
            ps.setLong(9, event.getKafkaOffset());
            ps.setString(10, event.getPayload());
            ps.setString(11, event.getProcessingStatus());
            ps.setString(12, event.getErrorMessage());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public Optional<RawSearchEvent> findByKafkaPosition(String topic, int partition, long offset) {
        String sql = """
            SELECT id, event_key, user_id, query, country,
                   occurred_at, received_at,
                   kafka_topic, kafka_partition, kafka_offset,
                   payload, processing_status, error_message
            FROM raw_search_events
            WHERE kafka_topic = ? AND kafka_partition = ? AND kafka_offset = ?
            """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> RawSearchEvent.builder()
                        .id(rs.getLong("id"))
                        .eventKey(rs.getString("event_key"))
                        .userId(rs.getString("user_id"))
                        .query(rs.getString("query"))
                        .country(rs.getString("country"))
                        .occurredAt(toInstant(rs.getTimestamp("occurred_at")))
                        .receivedAt(toInstant(rs.getTimestamp("received_at")))
                        .kafkaTopic(rs.getString("kafka_topic"))
                        .kafkaPartition(rs.getInt("kafka_partition"))
                        .kafkaOffset(rs.getLong("kafka_offset"))
                        .payload(rs.getString("payload"))
                        .processingStatus(rs.getString("processing_status"))
                        .errorMessage(rs.getString("error_message"))
                        .build(),
                topic, partition, offset
        ).stream().findFirst();
    }

    public void updateStatusAndError(Long id, String status, String errorMessage) {
        String sql = """
            UPDATE raw_search_events
            SET processing_status = ?, error_message = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, status, errorMessage, id);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
