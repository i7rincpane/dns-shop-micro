package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import ru.nvkz.common.ReactiveTraceExecutor;
import ru.nvkz.configuration.OutboxProperties;
import ru.nvkz.domain.OutboxEvent;
import ru.nvkz.repository.OutboxRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ru.nvkz.configuration.OutboxProperties.class)
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final KafkaSender<String, String> sender;
    private final ReactiveTraceExecutor traceExecutor;
    private final OutboxProperties outboxProperties;


    public Mono<Void> processPendingEvents() {
        return outboxRepository.findAllByProcessedFalse()
                .limitRate(outboxProperties.getLimitRate())
                .flatMap(this::sendEventToKafka)
                .then();
    }

    private Mono<OutboxEvent> sendEventToKafka(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                outboxProperties.getTopic(),
                event.getAggregateId(),
                event.getPayload().asString()
        );

        return traceExecutor.executeWithNextKafkaSpan(
                event.getTraceId(),
                event.getSpanId(),
                "order-outbox-publish",
                record,
                () -> sender.send(Mono.just(SenderRecord.create(record, event.getId())))
                        .next()
                        .flatMap(result -> {
                            if (result.exception() == null) {
                                log.info("Event {} successfully sent to Kafka", event.getId());
                                event.setProcessed(true);
                                return outboxRepository.save(event);
                            } else {
                                log.error("Error sending to Kafka for {}: {}",
                                        event.getId(),
                                        result.exception().getMessage());
                                return Mono.empty();
                            }
                        })
        );
    }
}
