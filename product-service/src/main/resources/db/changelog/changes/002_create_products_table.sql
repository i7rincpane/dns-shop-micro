--liquibase formatted sql

--changeset productservice:2

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    attributes JSONB DEFAULT '{}',
    description TEXT,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_attributes ON products USING GIN (attributes);