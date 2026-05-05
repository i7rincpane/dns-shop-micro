package ru.nvkz.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import ru.nvkz.domain.User;
import ru.nvkz.dto.UserSearchFilter;

@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Flux<User> findAllByFilter(UserSearchFilter filter) {
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