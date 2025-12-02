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
