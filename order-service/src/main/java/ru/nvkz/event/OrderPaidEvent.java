package ru.nvkz.event;

public record OrderPaidEvent(Long orderId,
                             Long userId) {
}
