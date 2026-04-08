package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.*;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.exception.handler.OutOfStockException;
import ru.nvkz.mapper.ProductMapper;
import ru.nvkz.repository.CategoryRepository;
import ru.nvkz.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public Flux<ProductFullResponse> findAllById(List<Long> ids) {
        return productRepository.findAllById(ids);
    }

    public Mono<ProductFullResponse> findById(Long id) {
        return productRepository.findFullById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("error.product.notfound", id)));
    }

    public Flux<ProductFullResponse> findAllByFilter(ProductSearchRequest productSearchRequest, Integer pageSize, Integer pageNumber) {
        return productRepository.findAllByFilter(productSearchRequest, pageSize, pageNumber);
    }

    @Transactional
    public Mono<Product> create(ProductSaveDto dto) {
        return categoryRepository.existsById(dto.categoryId())
                .flatMap(exists -> exists ? productRepository.save(productMapper.toProduct(dto)) :
                        Mono.error(new NotFoundException("error.category.notfound", dto.categoryId())));
    }

    @Transactional
    public Mono<Product> update(Long id, ProductUpdateDto dto) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("error.product.notfound", id)))
                .flatMap(product -> {
                    productMapper.updateProductFromDto(dto, product);
                    return productRepository.save(product);
                }).retryWhen(Retry.max(3).filter(ex -> ex instanceof OptimisticLockingFailureException));
    }

    public Mono<CategoryFiltersResponse> getFiltresByCategory(Long categoryId) {
        return productRepository.existsById(categoryId)
                .flatMap(exists -> exists
                        ? productRepository.getFiltersByCategory(categoryId)
                        : Mono.error(new NotFoundException("error.category.notfound", categoryId)));

    }

    @Transactional
    public Mono<Void> decreaseStock(List<StockUpdateRequest> requests) {
        return Flux.fromIterable(requests)
                .flatMap(request -> productRepository.decreaseStock(request.productId(),
                                request.quantity())
                        .flatMap(updatedRows -> {
                                    if (updatedRows == 0) {
                                        return Mono.error(new OutOfStockException("error.product.outofstock", request.productId(), request.quantity()));
                                    }
                                    return Mono.empty();
                                }
                        )
                )
                .then(); // просто сигнализируем об успехе,  превращаем Flux в Mono<Void>
    }

    @Transactional
    public Mono<Void> increaseStock(List<StockUpdateRequest> requests) {
        return Flux.fromIterable(requests)
                .flatMap(request -> productRepository.increaseStock(request.productId(),
                        request.quantity()))
                .then();
    }

}
