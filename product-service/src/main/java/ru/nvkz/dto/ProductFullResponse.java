package ru.nvkz.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductFullResponse(
        Long id,
        String name,
        BigDecimal price,
        String categoryName,
        Integer quantity,
        Map<String, Object> attributes
) {
}
