package ru.nvkz.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    BigDecimal totalPrice,
    List<OrderItemDto> items
) {}