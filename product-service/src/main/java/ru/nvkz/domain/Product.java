package ru.nvkz.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table("products")
public class Product {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private String description;
    private Integer quantity;
    private Map<String, Object> attributes;
    @Version
    private Long version;
}
