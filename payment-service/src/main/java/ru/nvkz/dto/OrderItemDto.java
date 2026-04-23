package ru.nvkz.dto;

import java.math.BigDecimal;

public record OrderItemDto(
    Long productId,
    String productName,
    BigDecimal priceAtPurchase,
    Integer quantity
) {}