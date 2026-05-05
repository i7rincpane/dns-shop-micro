package ru.nvkz.repository;

import reactor.core.publisher.Flux;
import ru.nvkz.domain.User;
import ru.nvkz.dto.UserSearchFilter;

public interface CustomUserRepository {

    Flux<User> findAllByFilter(UserSearchFilter filter);
}