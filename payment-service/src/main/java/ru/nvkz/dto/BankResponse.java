package ru.nvkz.dto;

import ru.nvkz.domain.PaymentStatus;

import java.math.BigDecimal;

public record BankResponse(
        Long orderId,
        Long userId,
        BigDecimal amount,
        PaymentStatus status
) {
}
