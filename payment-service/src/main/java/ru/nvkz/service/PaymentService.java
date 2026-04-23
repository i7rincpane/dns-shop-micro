package ru.nvkz.service;

import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.OutboxEvent;
import ru.nvkz.domain.OutboxEventType;
import ru.nvkz.domain.Payment;
import ru.nvkz.domain.PaymentStatus;
import ru.nvkz.dto.BankResponse;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.repository.OutboxRepository;
import ru.nvkz.repository.PaymentRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Mono<Payment> processWebhook(BankResponse bankResponse) {
        return paymentRepository.findByOrderId(bankResponse.orderId())
                .flatMap(payment -> {

                    if (payment.getStatus() == bankResponse.status()) { // для идемпотентности, если банк пришлет два раза сообщение для одного заказа, второй раз сохранять и отправлять сообщение в оутбокс не будем
                        log.info("Payment for order {} already has the status {}", bankResponse.orderId(), bankResponse.status());
                        return Mono.just(payment);
                    }

                    payment.setStatus(bankResponse.status());
                    return paymentRepository.save(payment)
                            .flatMap(saved -> outboxRepository.insert(mapToOutbox(saved)).thenReturn(saved));
                })
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("error.order.notfound", bankResponse.orderId()))));
 /*               .switchIfEmpty(Mono.defer(() -> paymentRepository.save(Payment.builder() // Сохраняем если кафка затупит, и ответ об оплате с банка придет раньше. Defer что бы сразу не вычислять аргументы(сохранение в бд), а лениво, только если не нашлась оплата.
                                .status(PaymentStatus.SUCCESS)
                                .userId(bankResponse.userId())
                                .orderId(bankResponse.orderId())
                                .amount(bankResponse.amount())
                                .build()))
                        .flatMap(saved -> outboxRepository.insert(mapToOutbox(saved)).thenReturn(saved))
                ); */
    }

    private OutboxEvent mapToOutbox(Payment savedPayment) {
        PaymentEvent payload = new PaymentEvent(
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getStatus());

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setType(savedPayment.getStatus() == PaymentStatus.SUCCESS
                ? OutboxEventType.PAYMENT_SUCCESS
                : OutboxEventType.PAYMENT_FAILED);
        outboxEvent.setAggregateId(savedPayment.getId().toString());
        outboxEvent.setPayload(Json.of(objectMapper.writeValueAsString(payload)));
        return outboxEvent;
    }
}
