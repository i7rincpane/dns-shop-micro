package ru.nvkz.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.event.PaymentStatus;
import ru.nvkz.service.OrderService;


@Slf4j
@Component
public class SuccessPaymentStrategy extends BasePaymentStrategy {

    public SuccessPaymentStrategy(OrderService orderService) {
        super(orderService);
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.SUCCESS;
    }

    @Override
    public Mono<Void> handle(PaymentEvent event) {
        return orderService.markAsPaid(event.orderId())
                .doOnSuccess(order -> log.info("Order {} has been successfully transferred to the PAID status",
                        event.orderId()))
                .then();
    }
}
