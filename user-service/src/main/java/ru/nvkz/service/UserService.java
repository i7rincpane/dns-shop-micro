package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;
import ru.nvkz.dto.UserSearchRequest;
import ru.nvkz.dto.UserUpdateDto;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.mapper.UserMapper;
import ru.nvkz.repository.UserProfileRepository;
import ru.nvkz.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final R2dbcEntityTemplate template;

    public Mono<UserFullInfo> findById(Long id) {
        return userRepository.findById(id)
                .zipWith(userProfileRepository.findByUserId(id), userMapper::toFullInfo) //параллельно выполняю два запроса, и объединяю их результат
                .switchIfEmpty(Mono.error(new NotFoundException("error.user.notfound", id)));
    }

    @Transactional
    public Mono<User> create(RegistrationRequest request) {
        User user = userMapper.toUser(request);
        return template.insert(user)
                .flatMap(savedUser -> {
                    UserProfile profile = userMapper.toProfile(request);
                    profile.setUserId(savedUser.id());
                    return template.insert(profile)
                            .thenReturn(savedUser);
                });
    }

    public Mono<UserProfile> updateProfile(Long userId, UserUpdateDto updateDto) {
        return userProfileRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("error.profile.notfound", userId)))
                .flatMap(profile -> {
                    userMapper.updateProfileFromDto(updateDto, profile);
                    return template.update(profile);
                }).retryWhen(Retry.max(3).filter(ex -> ex instanceof OptimisticLockingFailureException));

    }

    public Flux<User> findAllByFilter(UserSearchRequest filter) {
        Criteria criteria = Criteria.empty();

        if (filter.email() != null) {
            criteria = criteria.and("email").is(filter.email());
        }

        if (filter.role() != null) {
            criteria = criteria.and("role").is(filter.role());
        }

        if (filter.namePart() != null) {
            criteria = criteria.and("username").like("%" + filter.namePart() + "%");
        }

        return template.select(User.class)
                .matching(Query.query(criteria))
                .all();
    }
}
