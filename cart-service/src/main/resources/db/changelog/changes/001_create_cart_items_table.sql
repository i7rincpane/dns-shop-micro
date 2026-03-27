--liquibase formatted sql

--changeset cartservice:1

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    is_selected BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_user_product UNIQUE (user_id, product_id)
)