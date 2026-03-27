package ru.nvkz.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemDto> items,
        BigDecimal totalSelectedPrice
) {
}
