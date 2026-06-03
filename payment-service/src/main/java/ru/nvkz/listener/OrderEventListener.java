package ru.nvkz.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import ru.nvkz.common.ReactiveTraceExecutor;
import ru.nvkz.domain.Payment;
import ru.nvkz.domain.PaymentStatus;
import ru.nvkz.event.OrderCreatedEvent;
import ru.nvkz.event.OrderEventType;
import ru.nvkz.repository.PaymentRepository;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener implements CommandLineRunner {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper objectMapper;
    @Value("${app.order-events-topic.limitRate}")
    private int limitRate;
    private final PaymentRepository paymentRepository;

    private final ReactiveTraceExecutor traceExecutor;

    @Override
    public void run(String... args) throws Exception {
        kafkaReceiver.receive()
                .limitRate(limitRate)
                .flatMap(record ->
                        traceExecutor.executeAsKafkaConsumer(
                                record.headers(),
                                "order-payment-init",
                                "order-payment-init--consumer",
                                record.topic(),
                                () -> Mono.fromCallable(() ->
                                                objectMapper.readValue(record.value(), OrderCreatedEvent.class))
                                        .filter(event -> event.type()
                                                .equals(OrderEventType.ORDER_CREATED))
                                        .flatMap(event -> paymentRepository
                                                .findByOrderId(event.orderId())
                                                .switchIfEmpty(paymentRepository.save(Payment.builder()
                                                        .orderId(event.orderId())
                                                        .userId(event.userId())
                                                        .amount(event.totalPrice())
                                                        .status(PaymentStatus.PENDING)
                                                        .build())))
                                        .doOnSuccess(v -> {
                                            long offset = record.offset();
                                            record.receiverOffset().acknowledge();
                                            log.info("Items successfully processed, partition {}, offset {} confirmed",
                                                    record.partition(), offset);
                                        })

                                        .onErrorResume(ex -> {
                                            log.error("Order processing error: {}", ex.getMessage());
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
}
