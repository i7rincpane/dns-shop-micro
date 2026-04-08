package ru.nvkz.dto;

import java.util.List;

public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    List<OrderItemDto> items
) {}