package ru.nvkz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

public record ProductSaveDto(
        @NotNull(message = "{product.categoryid.notnull}")
        Long categoryId,
        @NotBlank(message = "{product.name.notblank}")
        String name,
        @NotNull(message = "{product.price.notnull}")
        @Positive(message = "{product.price.positive}")
        BigDecimal price,
        String description,
        Map<String, Object> attributes
) {
}