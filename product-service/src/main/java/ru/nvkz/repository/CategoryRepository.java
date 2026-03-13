package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.nvkz.domain.Category;

@Repository
public interface CategoryRepository extends R2dbcRepository<Category, Long> {
}
