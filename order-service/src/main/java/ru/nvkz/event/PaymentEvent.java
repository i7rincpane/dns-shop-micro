package ru.nvkz.event;

public record PaymentEvent(Long orderId,
                           Long userId,
                           PaymentStatus status) {
}
