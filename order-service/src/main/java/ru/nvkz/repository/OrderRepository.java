package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.nvkz.domain.Order;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
}
