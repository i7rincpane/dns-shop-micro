package ru.nvkz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.event.PaymentStatus;
import ru.nvkz.repository.ProcessedEventRepository;
import ru.nvkz.service.strategy.PaymentStrategy;
import ru.nvkz.util.IdempotencyCache;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentProcessingService {

    private final ProcessedEventRepository eventRepository;
    private final IdempotencyCache idempotencyCache;
    private final Map<PaymentStatus, PaymentStrategy> strategyByPaymentStatus;

    public PaymentProcessingService(
            List<PaymentStrategy> paymentStrategies,
            IdempotencyCache idempotencyCache,
            ProcessedEventRepository eventRepository) {
        this.idempotencyCache = idempotencyCache;
        this.eventRepository = eventRepository;
        this.strategyByPaymentStatus = new EnumMap<>(PaymentStatus.class);
        paymentStrategies.forEach(strategy ->
                strategyByPaymentStatus.put(strategy.getStatus(), strategy));
    }

    @Transactional
    public Mono<Void> process(PaymentEvent paymentEvent) {
        final UUID eventId = paymentEvent.eventId();

        if (idempotencyCache.contains(paymentEvent.eventId())) {
            log.info("Event {} found in cache, skipping", eventId);
            return Mono.empty();
        } // должен добавить в локальный кеш, второй раз не пропустить

        return eventRepository.insertIfAbsent(eventId)  //
                .flatMap(rowUpdated -> {
                    if (rowUpdated == 0) {
                        idempotencyCache.put(eventId);
                        log.warn("Event {} found in DB but not in cache, skipping",
                                eventId); // должен добавить в базу, второй раз не пропустить
                        return Mono.empty();
                    }

                    PaymentStrategy strategy = strategyByPaymentStatus.get(paymentEvent.status());
                    if (strategy == null) {
                        return Mono.error(new IllegalStateException("Unknown status: " + paymentEvent.status()));
                    } // должен не пропустить если нет стратегии

                    return strategy.handle(paymentEvent)
                            .doOnSuccess(v -> {
                                idempotencyCache.put(eventId);
                                // должен выполнить одну из двух стратегий
                                log.info("Event {} processed and cached", eventId);
                            });
                });
    }
}
