--liquibase formatted sql

--changeset orderservice:5
ALTER TABLE outbox_events ADD COLUMN trace_id VARCHAR(64);
ALTER TABLE outbox_events ADD COLUMN span_id VARCHAR(64);