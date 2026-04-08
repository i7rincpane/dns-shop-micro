package ru.nvkz.dto;

import java.math.BigDecimal;

public record CartItemDto(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity,
        boolean isSelected,
        BigDecimal subTotal
) {

    public CartItemDto {
        if (price == null) {
            price = BigDecimal.ZERO;
        }

        subTotal = price.multiply(new BigDecimal(quantity));
    }

    public CartItemDto(Long productId,
                       String productName,
                       BigDecimal price,
                       int quantity,
                       boolean isSelected) {
        this(productId, productName, price, quantity, isSelected, null);
    }
}
