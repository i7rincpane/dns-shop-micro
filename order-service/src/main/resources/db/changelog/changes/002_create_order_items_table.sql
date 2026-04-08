--liquibase formatted sql

--changeset orderservice:2
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price_at_purchase NUMERIC(19,2) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT uk_order_product UNIQUE (order_id, product_id)
);