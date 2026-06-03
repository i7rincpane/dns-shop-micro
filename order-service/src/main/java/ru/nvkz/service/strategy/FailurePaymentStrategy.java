package ru.nvkz.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.event.PaymentStatus;
import ru.nvkz.service.OrderService;


@Slf4j
@Component
public class FailurePaymentStrategy extends BasePaymentStrategy {

    public FailurePaymentStrategy(OrderService orderService) {
        super(orderService);
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.FAILED;
    }

    @Override
    public Mono<Void> handle(PaymentEvent event) {
        return orderService.compensateOrder(event.orderId())
                .doOnSuccess(order -> log.warn("The compensation of the order {} is completed. Status: CANCELLED",
                        event.orderId()))
                .then();
    }
}