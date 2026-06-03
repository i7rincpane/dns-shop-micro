package ru.nvkz.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;
import ru.nvkz.dto.UserUpdateDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toUser(RegistrationRequest request);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserProfile toProfile(RegistrationRequest request);

    UserFullInfo toFullInfo(User user, UserProfile profile);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "version", ignore = true)
    //Если нулл в дто, до игнорируем
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromDto(UserUpdateDto dto, @MappingTarget UserProfile profile);

}
