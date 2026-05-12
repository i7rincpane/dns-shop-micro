package ru.nvkz.scheduler;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
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
    private final Tracer tracer;
    private final Propagator propagator;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay}")
    public void processOutbox() {
        outboxRepository.findAllByProcessedFalse()
                .limitRate(limitRate)
                .flatMap(event -> {

                    Span kafkaSpan = tracer.spanBuilder()
                            .setParent(tracer.traceContextBuilder()
                                    .traceId(event.getTraceId())
                                    .spanId(event.getSpanId())
                                    .sampled(true).build())
                            .kind(Span.Kind.PRODUCER)
                            .name("payment-outbox-publish")
                            .tag("kafka.topic", topicName)
                            .start();

                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            topicName,
                            event.getAggregateId(),
                            event.getPayload().asString());

                    propagator.inject(kafkaSpan.context(), record, (rec, key, val) -> {
                        rec.headers().add(key, val.getBytes());
                    }); // внедряем контекст трассировки в сообщение кафка в виде заголовков

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
                                    log.error("Error sending to Kafka for {}: {}",
                                            event.getId(),
                                            result.exception().getMessage());
                                    return Mono.empty();
                                }
                            });
                })
                .subscribe();
    }
}
