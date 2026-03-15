package ru.nvkz.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductUpdateDto(
        String name,
        BigDecimal price,
        String description,
        Long categoryId,
        Map<String, Object> attributes
)
{}
