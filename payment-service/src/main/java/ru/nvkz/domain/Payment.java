package ru.nvkz.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@Table("payments")
@Builder
public class Payment {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    private Long orderId;
    private Long userId;
    private PaymentStatus status;
    private BigDecimal amount;
    private OffsetDateTime createdAt;
    @Version
    private Long version;
}
