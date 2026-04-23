package ru.nvkz.event;

import ru.nvkz.domain.PaymentStatus;

public record PaymentEvent(Long orderId,
                           Long userId,
                           PaymentStatus status) {
}
