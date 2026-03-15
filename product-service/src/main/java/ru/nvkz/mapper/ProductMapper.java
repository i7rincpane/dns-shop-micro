package ru.nvkz.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.ProductSaveDto;
import ru.nvkz.dto.ProductUpdateDto;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    public abstract Product toProduct(ProductSaveDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    public abstract void updateProductFromDto(ProductUpdateDto dto, @MappingTarget Product product);

    @BeforeMapping
    protected void mergeAttributes(ProductUpdateDto dto, @MappingTarget Product product) {
        if (dto.attributes() != null && product.getAttributes() != null) {
            Map<String, Object> merged = new HashMap<>(product.getAttributes());
            merged.putAll(dto.attributes());
            product.setAttributes(merged);
        }
    }

}
