package ru.nvkz.event;

import ru.nvkz.domain.OrderEventType;

public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        Reason reason,
        OrderEventType type
) {
    public enum Reason {
        PAYMENT_FAILED
    }
}

