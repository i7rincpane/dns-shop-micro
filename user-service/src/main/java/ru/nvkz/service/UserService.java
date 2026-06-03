package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;
import ru.nvkz.dto.UserSearchFilter;
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


    public Mono<UserFullInfo> findById(Long id) {
        return userRepository.findById(id)
                //Параллельно выполняю два запроса, и объединяю их результат
                .zipWith(userProfileRepository.findByUserId(id), userMapper::toFullInfo)
                .switchIfEmpty(Mono.error(new NotFoundException("error.user.notfound", id)));
    }

    @Transactional
    public Mono<User> create(RegistrationRequest request) {
        User user = userMapper.toUser(request);
        return userRepository.save(user)
                .flatMap(savedUser -> {
                    UserProfile profile = userMapper.toProfile(request);
                    profile.setUserId(savedUser.id());
                    return userProfileRepository.insert(profile)
                            .thenReturn(savedUser);
                });
    }

    public Mono<UserProfile> updateProfile(Long userId, UserUpdateDto updateDto) {
        return userProfileRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("error.profile.notfound", userId)))
                .flatMap(profile -> {
                    userMapper.updateProfileFromDto(updateDto, profile);
                    return userProfileRepository.save(profile);
                }).retryWhen(Retry.max(3).filter(ex -> ex instanceof OptimisticLockingFailureException));
    }

    public Flux<User> findAllByFilter(UserSearchFilter filter) {
        return userRepository.findAllByFilter(filter);
    }
}
