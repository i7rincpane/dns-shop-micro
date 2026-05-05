package ru.nvkz.event;

import ru.nvkz.domain.PaymentStatus;

import java.util.UUID;

public record PaymentEvent(UUID eventId,
                           Long orderId,
                           Long userId,
                           PaymentStatus status) {
}

