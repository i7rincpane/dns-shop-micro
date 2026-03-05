package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;
import ru.nvkz.dto.UserSearchRequest;
import ru.nvkz.mapper.UserMapper;
import ru.nvkz.repository.UserProfileRepository;
import ru.nvkz.repository.UserRepository;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final R2dbcEntityTemplate template;


    public Mono<UserFullInfo> findById(Long id) {
        return userRepository.findById(id)
                .zipWith(userProfileRepository.findByUserId(id), userMapper::toFullInfo)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")));
    }


    @Transactional
    public Mono<User> create(RegistrationRequest request) {
        User user = userMapper.toUser(request);
        return userRepository.save(user)
                .flatMap(savedUser -> {
                    UserProfile profile = userMapper.toProfile(request);
                    profile.setUserId(savedUser.id());
                    return userProfileRepository.save(profile)
                            .thenReturn(savedUser);
                });
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
