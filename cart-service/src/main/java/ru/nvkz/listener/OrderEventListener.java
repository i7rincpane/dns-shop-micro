package ru.nvkz.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import ru.nvkz.dto.OrderCreatedEvent;
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


    @Override
    public void run(String... args) throws Exception {
        kafkaReceiver.receive()
                .limitRate(limitRate)
                .flatMap(record -> Mono.fromCallable(() -> objectMapper.readValue(record.value(), OrderCreatedEvent.class))
                        .flatMap(orderCreatedEvent -> cartService.clearCart(orderCreatedEvent.userId(), getProductIds(orderCreatedEvent)))
                        .doOnSuccess(v -> {
                            long offset = record.offset();
                            record.receiverOffset().acknowledge();
                            log.info("Items successfully removed from cart, partition {}, offset {} confirmed", record.partition(), offset);
                        })
                        .onErrorResume(ex -> {
                            log.error("Skip bad message at offset {}: {}", record.offset(), ex.getMessage());
                            record.receiverOffset().acknowledge();
                            return Mono.empty();
                        })
                )
                .subscribe();
    }

    @NotNull
    private static List<Long> getProductIds(OrderCreatedEvent orderCreatedEvent) {
        return orderCreatedEvent.items().stream()
                .map(OrderItemDto::productId)
                .toList();
    }
}
