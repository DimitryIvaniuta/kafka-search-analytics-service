-- V4: Error log for failed processing attempts (business / technical).
-- Separate from raw_search_events to keep a clean, queryable error history.

CREATE TABLE IF NOT EXISTS search_event_processing_errors (
    id              BIGSERIAL PRIMARY KEY,

    raw_event_id    BIGINT REFERENCES raw_search_events(id) ON DELETE SET NULL,

    kafka_topic     VARCHAR(255) NOT NULL,
    kafka_partition INT          NOT NULL,
    kafka_offset    BIGINT       NOT NULL,

    error_type      VARCHAR(100) NOT NULL,  -- e.g. 'VALIDATION', 'DB_CONSTRAINT', 'UNKNOWN'
    error_message   TEXT         NOT NULL,
    stack_trace     TEXT,                   -- truncated stack trace if stored

    retry_count     INT          NOT NULL DEFAULT 0,
    last_retry_at   TIMESTAMPTZ,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processing_errors_occurred_at
    ON search_event_processing_errors (occurred_at);

CREATE INDEX IF NOT EXISTS idx_processing_errors_error_type
    ON search_event_processing_errors (error_type);

CREATE INDEX IF NOT EXISTS idx_processing_errors_raw_event_id
    ON search_event_processing_errors (raw_event_id);

SELECT s.name AS startpt,
       m.name AS middlept,
       e.name AS endpt
FROM (SELECT hut1 AS from_hut, hut2 AS to_hut
      FROM trails
      UNION
      SELECT hut2 AS from_hut, hut1 AS to_hut
      FROM trails) AS e1
         JOIN
     (SELECT hut1 AS from_hut, hut2 AS to_hut
      FROM trails
      UNION
      SELECT hut2 AS from_hut, hut1 AS to_hut
      FROM trails) AS e2
     ON e1.to_hut = e2.from_hut
         JOIN mountain_huts s ON s.id = e1.from_hut
         JOIN mountain_huts m ON m.id = e1.to_hut
         JOIN mountain_huts e ON e.id = e2.to_hut
WHERE s.altitude > m.altitude
  AND m.altitude > e.altitude;