package ru.nvkz.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import ru.nvkz.dto.ProductFullResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductClient {
    private final WebClient productWebClient;

    @CircuitBreaker(name = "productService")
    public Flux<ProductFullResponse> getProductAllById(List<Long> productIds) {
        return productWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/products")
                        .queryParam("ids", productIds)
                        .build())
                .retrieve()
                .bodyToFlux(ProductFullResponse.class);
    }
}
