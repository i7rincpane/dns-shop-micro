package ru.nvkz.mapper;

import org.mapstruct.Mapper;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(RegistrationRequest request);

    UserProfile toProfile(RegistrationRequest request);

    UserFullInfo toFullInfo(User user, UserProfile profile);
}
