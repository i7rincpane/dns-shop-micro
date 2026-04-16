package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.CartItem;

import java.util.List;

@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {
    Flux<CartItem> findByUserId(Long id);

    Mono<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    Mono<Void> deleteAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE user_id = :userId AND product_id IN (:productIds)")
    Mono<Void> deleteAllByUserIdAndProductIds(Long userId, List<Long> productIds);
}
