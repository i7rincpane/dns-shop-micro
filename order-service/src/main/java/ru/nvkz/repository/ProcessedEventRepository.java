package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.ProcessedEvent;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends R2dbcRepository<ProcessedEvent, UUID> {
    @Modifying
    @Query("""
            INSERT INTO processed_events(id) VALUES(:id)
            ON CONFLICT(id) DO NOTHING
            """)
    Mono<Integer> insertIfAbsent(UUID id);
}