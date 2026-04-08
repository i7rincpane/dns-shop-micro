package ru.nvkz.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @EqualsAndHashCode.Include
    private Long orderId;

    @EqualsAndHashCode.Include
    private Long productId;

    private String productName;
    private BigDecimal priceAtPurchase;

    private Integer quantity;

}
