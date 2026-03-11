--liquibase formatted sql

--changeset userservice:2
CREATE TABLE IF NOT EXISTS user_profile (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100),
    surname VARCHAR(100),
    phone VARCHAR(20),
    birth_date DATE
);