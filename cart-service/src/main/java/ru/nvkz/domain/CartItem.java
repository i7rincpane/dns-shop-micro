package ru.nvkz.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table("cart_items")
public class CartItem {

    @Id
    private Long id;

    @EqualsAndHashCode.Include
    private Long userId;

    @EqualsAndHashCode.Include
    private Long productId;

    private Integer quantity;
    private boolean isSelected;

    @Version
    private Long version;
}
