package com.github.dimitryivaniuta.searchanalytics.repository;

import com.github.dimitryivaniuta.searchanalytics.model.RawSearchEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RawSearchEventRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<RawSearchEvent> ROW_MAPPER = new RawSearchEventRowMapper();

    /**
     * Inserts a row into raw_search_events and returns generated id.
     *
     * NOTE:
     *  - On Postgres with RETURN_GENERATED_KEYS you often get a map of *all* columns.
     *  - Using keyHolder.getKey() throws when multiple keys exist.
     *  - We therefore read the "id" column from keyHolder.getKeys() / getKeyList().
     */
    public Long save(RawSearchEvent event) {
        String sql = """
            INSERT INTO raw_search_events (
                event_key,
                user_id,
                query,
                country,
                occurred_at,
                received_at,
                kafka_topic,
                kafka_partition,
                kafka_offset,
                payload,
                processing_status,
                error_message
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            ON CONFLICT (kafka_topic, kafka_partition, kafka_offset)
            DO NOTHING
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int updated = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, event.getEventKey());
            ps.setString(i++, event.getUserId());
            ps.setString(i++, event.getQuery());
            ps.setString(i++, event.getCountry());
            ps.setTimestamp(i++, toTimestamp(event.getOccurredAt()));
            ps.setTimestamp(i++, toTimestamp(event.getReceivedAt()));
            ps.setString(i++, event.getKafkaTopic());
            ps.setInt(i++, event.getKafkaPartition());
            ps.setLong(i++, event.getKafkaOffset());
            ps.setString(i++, event.getPayload());
            ps.setString(i++, event.getProcessingStatus());
            ps.setString(i, event.getErrorMessage());
            return ps;
        }, keyHolder);


        // When DO NOTHING is triggered, updated == 0 and keyHolder has no key
        if (updated == 0) {
            return null; // already existed – caller can handle this if needed
        }

        // ---- IMPORTANT: extract "id" from key map, not getKey() ----
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            return ((Number) keys.get("id")).longValue();
        }

        // Fallback: some drivers populate keyList instead
        if (!keyHolder.getKeyList().isEmpty()) {
            Map<String, Object> first = keyHolder.getKeyList().get(0);
            if (first.containsKey("id")) {
                return ((Number) first.get("id")).longValue();
            }
        }

        throw new IllegalStateException("Failed to obtain generated id for raw_search_events");
    }

    /**
     * Find a raw event by its Kafka position – useful for idempotency/debug.
     */
    public Optional<RawSearchEvent> findByKafkaPosition(String topic, int partition, long offset) {
        String sql = """
            SELECT id,
                   event_key,
                   user_id,
                   query,
                   country,
                   occurred_at,
                   received_at,
                   kafka_topic,
                   kafka_partition,
                   kafka_offset,
                   payload,
                   processing_status,
                   error_message
            FROM raw_search_events
            WHERE kafka_topic = ? AND kafka_partition = ? AND kafka_offset = ?
            """;

        return jdbcTemplate
                .query(sql, ROW_MAPPER, topic, partition, offset)
                .stream()
                .findFirst();
    }

    /**
     * Update processing status + error message (used by services on success or failure).
     */
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

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    private static class RawSearchEventRowMapper implements RowMapper<RawSearchEvent> {
        @Override
        public RawSearchEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return RawSearchEvent.builder()
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
                    .build();
        }
    }
}
