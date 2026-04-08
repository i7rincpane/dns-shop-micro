package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.ProductFullResponse;

import java.util.List;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long>, ProductCustomRepository {

    @Query(value = """
            SELECT p.id, p.name, p.price, p.quantity, p.attributes,  c.name as category_name
                        FROM products p
                        JOIN categories c ON p.category_id = c.id
                        WHERE p.id = :id
            """)
    Mono<ProductFullResponse> findFullById(Long id);

    @Query(value = """
                 SELECT p.id, p.name, p.price, p.quantity, p.attributes,  c.name as category_name
                        FROM products p
                        JOIN categories c ON p.category_id = c.id
                        WHERE p.id IN (:ids)
            """)
    Flux<ProductFullResponse> findAllById(List<Long> ids);

    @Modifying
    @Query(value = """
            UPDATE products p
            SET quantity = quantity - :qty
            where p.id = :id and p.quantity >= :qty
            """)
    Mono<Integer> decreaseStock(Long id, Integer qty);

    @Modifying
    @Query(value = """
            UPDATE products p
            SET quantity = quantity + :qty
            where p.id = :id
            """)
    Mono<Integer> increaseStock(Long id, Integer qty);
}
