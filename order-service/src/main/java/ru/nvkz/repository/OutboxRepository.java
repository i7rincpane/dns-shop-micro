package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.nvkz.domain.OutboxEvent;

@Repository
public interface OutboxRepository extends R2dbcRepository<OutboxEvent, Long> {

    Flux<OutboxEvent> findAllByProcessedFalse();

}