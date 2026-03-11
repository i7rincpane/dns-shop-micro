--liquibase formatted sql

--changeset userservice:1
CREATE TABLE IF NOT EXISTS users (
    id  BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255)
);