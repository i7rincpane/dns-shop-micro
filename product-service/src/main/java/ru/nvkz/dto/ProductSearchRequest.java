package ru.nvkz.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ProductSearchRequest(
        String namePart,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> attrs
) {
    public Map<String, Object> getAttrsAsMap() {
        if (attrs == null || attrs.isEmpty()) return null;
        return attrs.stream()
                .map(word -> word.split(":"))
                .filter(array -> array.length == 2)
                .collect(Collectors.toMap(array -> array[0], array -> array[1]));

    }
}
