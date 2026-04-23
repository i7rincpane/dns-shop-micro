--liquibase formatted sql
--changeset paymentservice:2

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL, -- ID сущности
    type VARCHAR(100) NOT NULL, -- тип сообщения
    payload JSONB NOT NULL, -- данные
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_outbox_unprocess ON outbox_events(created_at) where processed = false;

