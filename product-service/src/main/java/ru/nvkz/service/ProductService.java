package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.CategoryFiltersResponse;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.dto.ProductSaveDto;
import ru.nvkz.dto.ProductSearchRequest;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.mapper.ProductMapper;
import ru.nvkz.repository.CategoryRepository;
import ru.nvkz.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

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

    public Mono<CategoryFiltersResponse> getFiltresByCategory(Long categoryId) {
        return productRepository.existsById(categoryId)
                .flatMap(exists -> exists
                        ? productRepository.getFiltersByCategory(categoryId)
                        : Mono.error(new NotFoundException("error.category.notfound", categoryId)));

    }
}
