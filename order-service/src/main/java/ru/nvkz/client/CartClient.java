package ru.nvkz.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.nvkz.dto.CartResponse;

@Component
@RequiredArgsConstructor
public class CartClient {
    private final WebClient cartWebClient;

    @CircuitBreaker(name = "dnsShopService")
    public Mono<CartResponse> getCartByUserId(Long userId) {
        return cartWebClient.get()
                .uri("/api/v1/cart")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .bodyToMono(CartResponse.class);
    }

    @CircuitBreaker(name = "dnsShopService")
    public Mono<Void> cleanCart(Long userId) {
        return cartWebClient.delete().uri("/api/v1/cart")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .bodyToMono(Void.class);
    }

}
