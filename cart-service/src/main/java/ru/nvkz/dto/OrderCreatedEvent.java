package ru.nvkz.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        OrderEventType type,
        Long orderId,
        Long userId,
        BigDecimal totalPrice,
        List<OrderItemDto> items
) {
}