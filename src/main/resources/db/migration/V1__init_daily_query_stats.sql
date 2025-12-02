-- V1: Initial schema for search analytics microservice.
-- Creates table for aggregated search stats and supporting indexes.
-- Aggregated statistics per day & query.

CREATE TABLE IF NOT EXISTS daily_query_stats (
    id    BIGSERIAL PRIMARY KEY,
    day   DATE     NOT NULL,
    query TEXT     NOT NULL,
    count BIGINT   NOT NULL DEFAULT 0,
    CONSTRAINT uq_daily_query_stats_day_query UNIQUE (day, query)
);

CREATE INDEX IF NOT EXISTS idx_daily_query_stats_day
    ON daily_query_stats (day);

CREATE INDEX IF NOT EXISTS idx_daily_query_stats_query
    ON daily_query_stats (query);
