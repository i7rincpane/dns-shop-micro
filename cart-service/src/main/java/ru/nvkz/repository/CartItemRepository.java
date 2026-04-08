package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.CartItem;

@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {
    Flux<CartItem> findByUserId(Long id);

    Mono<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    Mono<Void> deleteAllByUserId(Long userId);
}
