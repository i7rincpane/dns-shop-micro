package ru.nvkz.listener;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import ru.nvkz.domain.Payment;
import ru.nvkz.domain.PaymentStatus;
import ru.nvkz.event.OrderCreatedEvent;
import ru.nvkz.event.OrderEventType;
import ru.nvkz.repository.PaymentRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener implements CommandLineRunner {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper objectMapper;
    @Value("${app.order-events-topic.limitRate}")
    private int limitRate;
    private final PaymentRepository paymentRepository;
    private final Propagator propagator;
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;

    @Override
    public void run(String... args) throws Exception {
        kafkaReceiver.receive()
                .limitRate(limitRate)
                .flatMap(record -> {
                    Map<String, String> headers = new HashMap<>();
                    record.headers().forEach(h -> headers.put(h.key(), new String(h.value())));
                    var parentContext = propagator.extract(headers, Map::get);

                    Observation observation = Observation.createNotStarted("order-payment-init", observationRegistry)
                            .lowCardinalityKeyValue("kafka.topic", record.topic())
                            .contextualName("order-payment-init--consumer");

                    if (parentContext != null) {
                        observation.parentObservation(null);
                    }

                    return Mono.defer(() -> {
                                try (var ps = tracer.withSpan(parentContext.start())) {
                                    observation.start();
                                    try (var scope = observation.openScope()) {
                                        return Mono.fromCallable(() ->
                                                        objectMapper.readValue(record.value(),
                                                                OrderCreatedEvent.class))
                                                .filter(event ->
                                                        event.type().equals(OrderEventType.ORDER_CREATED))
                                                .flatMap(event ->
                                                        paymentRepository.findByOrderId(event.orderId())
                                                                .switchIfEmpty(paymentRepository.save(Payment.builder()
                                                                        .orderId(event.orderId())
                                                                        .userId(event.userId())
                                                                        .amount(event.totalPrice())
                                                                        .status(PaymentStatus.PENDING)
                                                                        .build())))
                                                .doOnSuccess(v -> {
                                                    long offset = record.offset();
                                                    record.receiverOffset().acknowledge();
                                                    log.info("Items successfully processed, partition {}, " +
                                                                    "offset {} confirmed",
                                                            record.partition(), offset);
                                                })
                                                .onErrorResume(ex -> {
                                                    log.error("Order processing error: {}", ex.getMessage());
                                                    return Mono.empty();
                                                })
                                                .doFinally(signal -> observation.stop());
                                    }
                                }
                            })
                            .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
                })
                .subscribe();
    }
}
