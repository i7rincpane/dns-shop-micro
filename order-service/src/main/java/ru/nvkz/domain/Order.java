package ru.nvkz.domain;

import lombok.AllArgsConstructor;
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
@Table("orders")
public class Order {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private OffsetDateTime createdAt;
    @Version
    private Long version;
}
