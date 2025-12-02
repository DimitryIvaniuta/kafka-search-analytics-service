-- V3: Outbox table for publishing downstream events in a transactional way.
-- Used if you later need to emit events to other topics/services after DB commit.

CREATE TABLE IF NOT EXISTS search_event_outbox (
    id             BIGSERIAL PRIMARY KEY,

    aggregate_type VARCHAR(100)  NOT NULL,  -- e.g. 'DailyQueryStat'
    aggregate_id   VARCHAR(200)  NOT NULL,  -- e.g. daily_query_stats.id or composite key

    event_type     VARCHAR(100)  NOT NULL,  -- e.g. 'SEARCH_STATS_UPDATED'
    payload        JSONB         NOT NULL,  -- serialized event body
    headers        JSONB,                   -- optional metadata / tracing headers
    partition_key  VARCHAR(200),            -- key for Kafka partitioning if needed

    status         VARCHAR(32)   NOT NULL DEFAULT 'NEW',
    -- NEW | PUBLISHED | FAILED
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ,
    last_error     TEXT
);

-- Producer picks NEW events by created_at asc
CREATE INDEX IF NOT EXISTS idx_outbox_status_created_at
    ON search_event_outbox (status, created_at);

-- For inspecting events for a given aggregate
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON search_event_outbox (aggregate_type, aggregate_id);
