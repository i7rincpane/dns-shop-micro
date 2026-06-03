package ru.nvkz.service.strategy;

import reactor.core.publisher.Mono;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.event.PaymentStatus;

public interface PaymentStrategy {

    PaymentStatus getStatus();

    Mono<Void> handle(PaymentEvent event);

}
