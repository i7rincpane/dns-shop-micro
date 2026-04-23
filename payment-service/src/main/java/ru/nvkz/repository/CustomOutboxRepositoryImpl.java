package ru.nvkz.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.OutboxEvent;

@RequiredArgsConstructor
public class CustomOutboxRepositoryImpl implements CustomOutboxRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<OutboxEvent> insert(OutboxEvent event) {
        return template.insert(event);
    }
}
