package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.ProductFullResponse;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long>, ProductCustomRepository  {

    @Query(value = """
        SELECT p.id, p.name, p.price, p.attributes,  c.name as category_name
                    FROM products p
                    JOIN categories c ON p.category_id = c.id
                    WHERE p.id = :id
        """)
    Mono<ProductFullResponse> findFullById(Long id);
}
