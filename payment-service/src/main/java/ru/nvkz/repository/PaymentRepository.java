package ru.nvkz.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Payment;

@Repository
public interface PaymentRepository extends R2dbcRepository<Payment, Long> {
    Mono<Payment> findByOrderId(Long orderId);
}
