package ru.nvkz.mapper;

import org.mapstruct.*;
import ru.nvkz.domain.CartItem;
import ru.nvkz.dto.CartItemUpdateDto;

@Mapper(componentModel = "spring")
public interface CartItemMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "selected", source = "isSelected")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCartItemFromDto(CartItemUpdateDto updateDto, @MappingTarget CartItem cartItem);
}
