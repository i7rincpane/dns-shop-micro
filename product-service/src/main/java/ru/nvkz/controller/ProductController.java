package ru.nvkz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.CategoryFiltersResponse;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.dto.ProductSaveDto;
import ru.nvkz.dto.ProductSearchRequest;
import ru.nvkz.service.ProductService;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Flux<ProductFullResponse> getAll(ProductSearchRequest productSearchRequest, @RequestParam(defaultValue = "20") Integer pageSize, @RequestParam(defaultValue = "0") Integer pageNumber) {
        return productService.findAllByFilter(productSearchRequest, pageSize, pageNumber);
    }

    @GetMapping("/{id}")
    public Mono<ProductFullResponse> getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> create(@Valid @RequestBody ProductSaveDto product) {
        return productService.create(product);
    }

    @GetMapping("/filters")
    public Mono<CategoryFiltersResponse> getFilters(@RequestParam Long categoryId) {
        return productService.getFiltresByCategory(categoryId);
    }
}
