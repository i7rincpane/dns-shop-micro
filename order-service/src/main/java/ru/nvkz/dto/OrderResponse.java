package ru.nvkz.dto;

import ru.nvkz.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    OrderStatus status,
    BigDecimal totalPrice,
    OffsetDateTime createdAt,
    List<OrderItemDto> items
) {}