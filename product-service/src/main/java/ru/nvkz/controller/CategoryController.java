package ru.nvkz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Category;
import ru.nvkz.repository.CategoryRepository;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Category> create(@RequestBody Category category) {
        return categoryRepository.save(category);
    }

    @GetMapping
    public Flux<Category> getAll() {
        return categoryRepository.findAll();
    }

}
