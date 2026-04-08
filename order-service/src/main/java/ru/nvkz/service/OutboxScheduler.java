package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import ru.nvkz.repository.OutboxRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaSender<String, String> sender;

    @Value("${app.outbox.topic}")
    private String topicName;

    @Value("${app.outbox.limitRate}")
    private int limitRate;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay}")
    public void processOutbox() {
        outboxRepository.findAllByProcessedFalse()
                .limitRate(limitRate)
                .flatMap(event -> {

                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            topicName,
                            event.getAggregateId(),
                            event.getPayload().asString());

                    SenderRecord<String, String, UUID> reactiveRecord = SenderRecord.create(
                            record
                            , event.getId());


                    return sender.send(Mono.just(reactiveRecord))
                            .next()
                            .flatMap(result -> {
                                if (result.exception() == null) {
                                    log.info("Event {} successfully sent to Kafka", event.getId());
                                    event.setProcessed(true);
                                    return outboxRepository.save(event);
                                } else {
                                    log.error("Error sending to Kafka for {}: {}", event.getId(), result.exception().getMessage());
                                    return Mono.empty();
                                }
                            });
                })
                .subscribe();
    }
}
