package ru.nvkz.domain;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("outbox_events")
public class OutboxEvent {
    @Id
    @EqualsAndHashCode.Include
    private UUID id;
    private String aggregateId;
    private PaymentEventType type;
    private Json payload;
    private OffsetDateTime createdAt;
    private boolean processed;
    private String traceId;
    private String spanId;
}