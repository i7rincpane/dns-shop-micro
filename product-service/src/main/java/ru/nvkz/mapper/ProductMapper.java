package ru.nvkz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.ProductSaveDto;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target =  "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toProduct(ProductSaveDto dto);

}
