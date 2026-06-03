package ru.nvkz.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.dto.CategoryFiltersResponse;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.dto.ProductSearchRequest;

public interface ProductCustomRepository {

    Flux<ProductFullResponse> findAllByFilter(ProductSearchRequest filter, Integer pageSize, Integer pageNumber);
    Mono<CategoryFiltersResponse> getFiltersByCategory(Long categoryId);
}
