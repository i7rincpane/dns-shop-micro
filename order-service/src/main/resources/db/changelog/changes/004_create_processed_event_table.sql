--liquibase formatted sql
--changeset orderservice:4
CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

