package ru.nvkz.repository;

import reactor.core.publisher.Mono;
import ru.nvkz.domain.UserProfile;

public interface CustomUserProfileRepository {

    Mono<UserProfile> insert(UserProfile profile);
}
