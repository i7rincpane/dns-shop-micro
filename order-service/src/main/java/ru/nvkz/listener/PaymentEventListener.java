package ru.nvkz.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.event.PaymentStatus;
import ru.nvkz.service.OrderService;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener implements CommandLineRunner {


    private final KafkaReceiver<String, String> kafkaReceiver;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    @Value("${app.payment-events-topic.limitRate}")
    private int limitRate;

    @Override
    public void run(String... args) throws Exception {
        kafkaReceiver.receive()
                .flatMap(record -> Mono.fromCallable(() -> objectMapper.readValue(record.value(), PaymentEvent.class))
                        .flatMap(paymentEvent ->

                                switch (paymentEvent.status()) {
                                    case PaymentStatus.SUCCESS -> handleSuccess(paymentEvent);
                                    case PaymentStatus.FAILED -> handleFailure(paymentEvent);
                                    default -> Mono.empty();
                                }
                        ).doOnSuccess(v -> record.receiverOffset().acknowledge())
                        .onErrorResume(ex -> {
                            log.error("Error processing payment event: {}", ex.getMessage());
                            return Mono.empty();
                        })
                )

                .subscribe();

    }

    private Mono<Void> handleSuccess(PaymentEvent event) {
        return orderService.markAsPaid(event.orderId())
                .doOnSuccess(order -> log.info("Order {} has been successfully transferred to the PAID status", event.orderId()))
                .then();
    }

    private Mono<Void> handleFailure(PaymentEvent event) {
        return orderService.compensateOrder(event.orderId())
                .doOnSuccess(order -> log.warn("The compensation of the order {} is completed. Status: CANCELLED", event.orderId()))
                .then();
    }


}
