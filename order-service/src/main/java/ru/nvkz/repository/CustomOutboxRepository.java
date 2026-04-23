package ru.nvkz.repository;

import reactor.core.publisher.Mono;
import ru.nvkz.domain.OutboxEvent;

public interface CustomOutboxRepository {
    Mono<OutboxEvent> insert(OutboxEvent event);
}
