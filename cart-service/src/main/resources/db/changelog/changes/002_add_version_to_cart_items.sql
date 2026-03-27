--liquibase formatted sql

--changeset cartservice:1

ALTER TABLE cart_items ADD COLUMN version INT DEFAULT 0;