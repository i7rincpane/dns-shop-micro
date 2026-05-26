package ru.nvkz.listener;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import ru.nvkz.common.ReactiveTraceExecutor;
import ru.nvkz.dto.OrderCreatedEvent;
import ru.nvkz.dto.OrderEventType;
import ru.nvkz.dto.OrderItemDto;
import ru.nvkz.service.CartService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener implements CommandLineRunner {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final CartService cartService;
    private final ObjectMapper objectMapper;

    @Value("${app.order-events-topic.limitRate}")
    private int limitRate;

    private final ReactiveTraceExecutor traceExecutor;

    @Override
    public void run(String... args) throws Exception {
        kafkaReceiver.receive()
                .limitRate(limitRate)
                .flatMap(record ->
                        traceExecutor.executeAsKafkaConsumer(
                                record.headers(),
                                "cart-clear-consumer",
                                "cart-clear-consumer--processor",
                                record.topic(),
                                () -> Mono.fromCallable(() -> objectMapper.readValue(record.value(),
                                                OrderCreatedEvent.class))
                                        .filter(event -> event.type()
                                                .equals(OrderEventType.ORDER_CREATED))
                                        .flatMap(orderCreatedEvent -> cartService.clearCart(
                                                        orderCreatedEvent.userId(),
                                                        getProductIds(orderCreatedEvent))
                                                .doOnSuccess(a -> log.info("Items successfully removed from cart"))
                                        )
                                        .doOnSuccess(v -> {
                                            long offset = record.offset();
                                            record.receiverOffset().acknowledge();
                                            log.info("Message processed or skipped at partition {}," +
                                                            " offset {} confirmed",
                                                    record.partition(), offset);
                                        })
                                        .onErrorResume(throwable -> {
                                            log.error("Skip bad message at offset {}: {}", record.offset(),
                                                    throwable.getMessage());
                                            record.receiverOffset().acknowledge();
                                            return Mono.empty();
                                        })
                        )
                )
                .onErrorResume(ex -> {
                    log.error("Kafka consumer flow failed", ex);
                    return Mono.empty();
                })
                .subscribe();
    }

    @NotNull
    private static List<Long> getProductIds(OrderCreatedEvent orderCreatedEvent) {
        return orderCreatedEvent.items().stream()
                .map(OrderItemDto::productId)
                .toList();
    }
}
