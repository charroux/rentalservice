-- Migration: V3__create_processed_events_table.sql
-- Purpose: Create table for idempotent event processing tracking
-- Phase: Phase 1 - Redis Streams Event-Driven Architecture
-- Description: 
--   Tracks which events have been processed by which consumers.
--   Ensures exactly-once semantics in event-driven microservices.
--   Prevents duplicate processing if consumers retry or crash/recover.

CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Composite unique constraint: each consumer processes each event only once
ALTER TABLE processed_events
    ADD CONSTRAINT uc_event_consumer UNIQUE (event_id, consumer_name);

-- Index for checking if event was already processed (idempotence check)
CREATE INDEX idx_processed_events_event_id 
    ON processed_events(event_id);

-- Index for consumer-specific queries (monitoring per consumer)
CREATE INDEX idx_processed_events_consumer_name 
    ON processed_events(consumer_name);

-- Index for time-based queries (cleanup old records)
CREATE INDEX idx_processed_events_processed_at 
    ON processed_events(processed_at DESC);

-- Index for error tracking (monitoring high-retry events)
CREATE INDEX idx_processed_events_retry_count 
    ON processed_events(retry_count DESC) 
    WHERE error_message IS NOT NULL;

COMMENT ON TABLE processed_events IS 
    'Tracks processed events to ensure idempotent handling by consumers. ' ||
    'Pattern: (event_id, consumer_name) unique = each consumer processes each event exactly once.';

COMMENT ON COLUMN processed_events.event_id IS 
    'Globally unique event identifier (UUID string). Used for deduplication.';

COMMENT ON COLUMN processed_events.consumer_name IS 
    'Name of the consumer that processed this event. ' ||
    'Allows different services to consume same event independently.';

COMMENT ON COLUMN processed_events.event_type IS 
    'Type of event (e.g., AuctionWon, AuctionLost). Used for debugging and auditing.';

COMMENT ON COLUMN processed_events.retry_count IS 
    'Number of retries before successful processing. ' ||
    '0 = succeeded on first attempt. Used for monitoring retry rates.';

COMMENT ON COLUMN processed_events.error_message IS 
    'Error message if event processing failed. ' ||
    'NULL if successful. Populated when event moved to DLQ after MAX_RETRIES.';
