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
import ru.nvkz.exception.handler.BadRequestException;
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
// для идемпотентности,
// если банк пришлет два раза сообщение для одного заказа,
// второй раз сохранять и отправлять сообщение в оутбокс не будем
                    if (payment.getStatus() == bankResponse.status()) {
                        return Mono.error(
                                new BadRequestException("error.order.already",
                                        new Object[]{bankResponse.orderId(), bankResponse.status()}));
                    }

                    payment.setStatus(bankResponse.status());
                    return paymentRepository.save(payment)
                            .flatMap(saved -> outboxRepository.insert(mapToOutbox(saved)).thenReturn(saved));
                })
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("error.order.notfound",
                        bankResponse.orderId()))));
 /*
 Сохраняем если кафка затупит, и ответ об оплате с банка придет раньше.
 Defer что бы сразу не вычислять аргументы(сохранение в бд), а лениво, только если не нашлась оплата.
              .switchIfEmpty(Mono.defer(() -> paymentRepository.save(Payment.builder()

                                .status(PaymentStatus.SUCCESS)
                                .userId(bankResponse.userId())
                                .orderId(bankResponse.orderId())
                                .amount(bankResponse.amount())
                                .build()))
                        .flatMap(saved -> outboxRepository.insert(mapToOutbox(saved)).thenReturn(saved))
                ); */
    }

    private OutboxEvent mapToOutbox(Payment savedPayment) {
        UUID eventId = UUID.randomUUID(); //Помещаю и в outboxEvent и PaymentEvent.
        // Outbox сам по себе дает гарантирую, что сообщение с этим eventId, повторно не создастся
        PaymentEvent payload = new PaymentEvent(
                eventId,
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getStatus());

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(eventId);
        outboxEvent.setType(savedPayment.getStatus() == PaymentStatus.SUCCESS
                ? OutboxEventType.PAYMENT_SUCCESS
                : OutboxEventType.PAYMENT_FAILED);
        outboxEvent.setAggregateId(savedPayment.getId().toString());
        outboxEvent.setPayload(Json.of(objectMapper.writeValueAsString(payload)));
        return outboxEvent;
    }
}
