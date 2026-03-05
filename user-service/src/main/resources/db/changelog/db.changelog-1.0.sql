--liquibase formatted sql

--changeset userservice:1
CREATE TABLE IF NOT EXISTS users (
    id  BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255)
);

--changeset userservice:2
CREATE TABLE IF NOT EXISTS user_profile (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100),
    surname VARCHAR(100),
    phone VARCHAR(20),
    birth_date DATE
);

--changeSet userservice:3
--INSERT INTO users (email, password) VALUES ('test_user', '123');