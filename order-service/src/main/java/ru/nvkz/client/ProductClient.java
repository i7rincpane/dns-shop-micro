package ru.nvkz.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.nvkz.dto.StockUpdateRequest;
import ru.nvkz.exception.handler.DnsShopException;
import ru.nvkz.exception.handler.OrderCreationException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final WebClient productWebClient;

    public Mono<Void> decrease(List<StockUpdateRequest> requests) {
        return productWebClient.post()
                .uri("/api/v1/products/stock/decrease")
                .bodyValue(requests)
                .retrieve().onStatus(httpStatusCode -> httpStatusCode.value() == 422,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new OrderCreationException(error))))
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Остатки успешно списаны для {} позиций", requests.size()));

    }

    public Mono<Void> increase(List<StockUpdateRequest> requests) {
        return productWebClient.post()
                .uri("/api/v1/products/stock/increase")
                .bodyValue(requests)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Остатки успешно возвращены на склад"));

    }

}
