package ru.nvkz.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.UserProfile;

@RequiredArgsConstructor
public class CustomUserProfileRepositoryImpl implements CustomUserProfileRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<UserProfile> insert(UserProfile profile) {
        return template.insert(profile);
    }
}
