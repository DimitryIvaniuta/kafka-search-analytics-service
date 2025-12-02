-- V2: Raw search events audit table.
-- Stores every Kafka message consumed for debug, replay, and compliance.

CREATE TABLE IF NOT EXISTS raw_search_events (
    id                BIGSERIAL PRIMARY KEY,
    event_key         VARCHAR(255),          -- Kafka key (userId/sessionId etc.)
    user_id           VARCHAR(255),
    query             TEXT,
    country           VARCHAR(8),
    occurred_at       TIMESTAMPTZ,           -- business event time from payload
    received_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- when service consumed it

    kafka_topic       VARCHAR(255) NOT NULL,
    kafka_partition   INT          NOT NULL,
    kafka_offset      BIGINT       NOT NULL,

    payload           JSONB        NOT NULL, -- original message as JSON
    processing_status VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED',
    -- RECEIVED | PROCESSED | SKIPPED | ERROR
    error_message     TEXT,

    CONSTRAINT uq_raw_search_events_kafka_message
    UNIQUE (kafka_topic, kafka_partition, kafka_offset)
);

-- For time-based queries and retention
CREATE INDEX IF NOT EXISTS idx_raw_search_events_received_at
    ON raw_search_events (received_at);

CREATE INDEX IF NOT EXISTS idx_raw_search_events_occurred_at
    ON raw_search_events (occurred_at);

-- For troubleshooting/idempotency by user / key
CREATE INDEX IF NOT EXISTS idx_raw_search_events_user_id
    ON raw_search_events (user_id);

CREATE INDEX IF NOT EXISTS idx_raw_search_events_event_key
    ON raw_search_events (event_key);

-- For operational dashboards
CREATE INDEX IF NOT EXISTS idx_raw_search_events_status
    ON raw_search_events (processing_status);
