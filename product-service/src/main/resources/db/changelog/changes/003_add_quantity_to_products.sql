--liquibase formatted sql

--changeset productservice:3
ALTER TABLE products ADD quantity int NOT NULL DEFAULT 0;