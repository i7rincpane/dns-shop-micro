package ru.nvkz.event;

import ru.nvkz.domain.OrderEventType;

public record OrderPaidEvent(Long orderId,
                             Long userId,
                             OrderEventType type) {
}
