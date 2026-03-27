package ru.nvkz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @NotNull(message = "{cart.product.notnull}")
        Long productId,
        @Positive(message = "{cart.quantity.positive}")
        int quantity)
{
}
