package ru.nvkz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.CategoryFiltersResponse;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.dto.ProductSaveDto;
import ru.nvkz.dto.ProductSearchRequest;
import ru.nvkz.dto.ProductUpdateDto;
import ru.nvkz.dto.StockUpdateRequest;
import ru.nvkz.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping(params = "ids")
    public Flux<ProductFullResponse> getAll(@RequestParam List<Long> ids) {
        return productService.findAllById(ids);
    }

    @GetMapping
    public Flux<ProductFullResponse> getAll(ProductSearchRequest productSearchRequest,
                                            @RequestParam(defaultValue = "20") Integer pageSize,
                                            @RequestParam(defaultValue = "0") Integer pageNumber) {
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

    @PatchMapping("/{id}")
    public Mono<Product> update(@PathVariable Long id, @RequestBody ProductUpdateDto dto) {
        return productService.update(id, dto);
    }

    @PostMapping("stock/decrease")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> decreaseStock(@RequestBody List<StockUpdateRequest> requests) {
        return productService.decreaseStock(requests);
    }

    @PostMapping("stock/increase")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> increaseStock(@RequestBody List<StockUpdateRequest> requests) {
        return productService.increaseStock(requests);
    }

}
