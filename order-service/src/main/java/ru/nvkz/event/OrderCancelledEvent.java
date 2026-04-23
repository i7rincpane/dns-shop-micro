package ru.nvkz.event;

public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        Reason reason
) {
    public enum Reason {
        PAYMENT_FAILED
    }
}

