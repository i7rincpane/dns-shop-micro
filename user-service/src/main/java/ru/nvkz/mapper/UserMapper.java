package ru.nvkz.mapper;

import org.mapstruct.*;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;
import ru.nvkz.dto.UserUpdateDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target =  "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toUser(RegistrationRequest request);

    @Mapping(target =  "userId", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserProfile toProfile(RegistrationRequest request);

    UserFullInfo toFullInfo(User user, UserProfile profile);

    @Mapping(target =  "userId", ignore = true) // нету
    @Mapping(target = "version", ignore = true)// чтоб не переписать версию из dto
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE) //если нулл в дто, до игнорируем
    void updateProfileFromDto(UserUpdateDto dto, @MappingTarget UserProfile profile);

}
