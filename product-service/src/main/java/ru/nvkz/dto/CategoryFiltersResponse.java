package ru.nvkz.dto;

import java.util.Collection;
import java.util.Map;

public record CategoryFiltersResponse(
        Map<String, Collection<FilterValue>> attributes
) {
}
