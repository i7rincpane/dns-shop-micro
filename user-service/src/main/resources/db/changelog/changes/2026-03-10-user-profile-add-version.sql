--liquibase formatted sql

--changeset userservice:3
ALTER TABLE user_profile ADD COLUMN version BIGINT NOT NULL DEFAULT 0;