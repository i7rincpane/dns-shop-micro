package ru.nvkz.event;

import java.util.UUID;

public record PaymentEvent(UUID eventId,
                           Long orderId,
                           Long userId,
                           PaymentStatus status) {
}
