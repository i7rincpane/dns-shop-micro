package ru.nvkz.repository;

import reactor.core.publisher.Flux;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.dto.ProductSearchRequest;

public interface ProductCustomRepository {

    Flux<ProductFullResponse> findAllByFilter(ProductSearchRequest filter,  Integer pageSize, Integer pageNumber);
}
