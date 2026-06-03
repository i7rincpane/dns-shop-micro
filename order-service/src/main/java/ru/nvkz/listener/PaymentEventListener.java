package ru.nvkz.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.retry.Retry;
import ru.nvkz.common.ReactiveTraceExecutor;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.service.PaymentProcessingService;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener implements CommandLineRunner {

    private final KafkaReceiver<String, String> receiver;
    private final ObjectMapper objectMapper;
    private final PaymentProcessingService processingService;
    private final Retry kafkaRetry;
    private final KafkaSender<String, String> sender;

    @Value("${app.payment-events-topic.limitRate}")
    private int limitRate;

    private final ReactiveTraceExecutor traceExecutor;

    @Override
    public void run(String... args) throws Exception {
        receiver.receive()
                .limitRate(limitRate)
                .flatMap(record ->
                        traceExecutor.executeAsKafkaConsumer(
                                record.headers(),
                                "order-payment-process",
                                "order-payment-process--consumer",
                                record.topic(),                     // Тег топика для фильтрации
                                () -> Mono.fromCallable(() -> objectMapper
                                                .readValue(record.value(), PaymentEvent.class))
                                        .flatMap(processingService::process)
                                        .retryWhen(kafkaRetry)
                                        .onErrorResume(ex -> {
                                            log.error("The message {} has been failed. Error: {}",
                                                    record.key(),
                                                    ex.getMessage());
                                            return sendToDlq(record, ex)
                                                    .then(Mono.fromRunnable(() ->
                                                            record.receiverOffset().acknowledge()));
                                        })
                        )
                )
                .onErrorResume(ex -> {
                    log.error("Kafka consumer flow failed", ex);
                    return Mono.empty();
                })
                .subscribe();
    }

    private Mono<Void> sendToDlq(ReceiverRecord<String, String> record, Throwable ex) {

        String dlqTopic = record.topic() + ".dlq";

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(
                dlqTopic,
                record.key(),
                record.value()
        );

        producerRecord.headers().add("x-dead-letter-reason", ex.getMessage().getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("x-original-topic", record.topic().getBytes(StandardCharsets.UTF_8));

        log.warn("Send order message {} to DLQ: {}", record.key(), ex.getMessage());

        return sender.send(Mono.just(SenderRecord.create(producerRecord, record.key())))
                .next() // так как Mono.just отправляем, то и результат, подтверждения которого мы ждем, всего один
                .flatMap(result -> {
                    if (result.exception() != null) {
                        // Ошибка отправки в кафку, нельзя подтверждать офсет.
                        log.error("Error sending to Kafka for {} {}: {}",
                                dlqTopic, record.key(), result.exception().getMessage());
                        return Mono.error(result.exception());
                    }
                    log.info("Message successfully added to the corrupted queue {} {}", dlqTopic, record.key());
                    return Mono.empty();
                });
    }

}
