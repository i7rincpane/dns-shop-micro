package ru.nvkz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.Order;
import ru.nvkz.domain.OrderStatus;
import ru.nvkz.domain.OutboxEvent;
import ru.nvkz.domain.OutboxEventType;
import ru.nvkz.domain.ProcessedEvent;
import ru.nvkz.event.PaymentEvent;
import ru.nvkz.event.PaymentStatus;
import ru.nvkz.util.IdempotencyCache;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentProcessingServiceIT extends BaseIntegrationTest {

    @Autowired
    private PaymentProcessingService processingService;

    @Autowired
    private IdempotencyCache idempotencyCache;

    @BeforeEach
    void init() {
        template.insert(new Order(1L, 1L, OrderStatus.NEW, BigDecimal.ZERO, null, null))
                .then(template.insert(
                        new Order(2L, 1L, OrderStatus.PAID, BigDecimal.ZERO, null, null)))
                .block();
    }

    @Test
    void shouldMarkAsPaid() {
        PaymentEvent paymentEvent = getPaymentEvent(1L);

        StepVerifier.create(processingService.process(paymentEvent))
                .verifyComplete();

        StepVerifier.create(template.selectOne(Query.query(Criteria.where("id")
                        .in(paymentEvent.orderId())), Order.class))
                .assertNext(actualOrder -> {
                            assertThat(actualOrder.getStatus())
                                    .isEqualTo(OrderStatus.PAID);
                        }
                ).as("Must change the status of the order from new to paid")
                .verifyComplete();

        StepVerifier.create(template.select(OutboxEvent.class).all().collectList())
                .assertNext(actualOutboxEvents -> {
                    assertThat(actualOutboxEvents).hasSize(1);
                    assertThat(actualOutboxEvents.getFirst().getAggregateId())
                            .isEqualTo(paymentEvent.orderId().toString());
                    assertThat(actualOutboxEvents.getFirst().getType()).isEqualTo(OutboxEventType.ORDER_PAID);
                })
                .as("Must create a payment message for the order")
                .verifyComplete();

        StepVerifier.create(template.select(ProcessedEvent.class).all().collectList())
                .assertNext(actualProcessedEvents -> {
                    assertThat(actualProcessedEvents).hasSize(1);
                    assertThat(actualProcessedEvents.getFirst().getId()).isEqualTo(paymentEvent.eventId());
                })
                .as("Must add cache to DB")
                .verifyComplete();

        assertThat(idempotencyCache.contains(paymentEvent.eventId()))
                .as("Should be added to the local cache")
                .isTrue();
    }

    @Test
    void shouldBeIdempotent() {
        Long orderId = 1L;
        PaymentEvent paymentEvent = getPaymentEvent(orderId);

        StepVerifier.create(processingService.process(paymentEvent))
                .verifyComplete();

        StepVerifier.create(processingService.process(paymentEvent))
                .verifyComplete();

        StepVerifier.create(template.select(ProcessedEvent.class).all().collectList())
                .assertNext(actualProcessedEvents -> {
                    assertThat(actualProcessedEvents).hasSize(1);
                })
                .as("Idempotency should work, and only one record will be preserved")
                .verifyComplete();

        StepVerifier.create(template.select(OutboxEvent.class).all().collectList())
                .assertNext(actualOutboxEvents -> {
                    assertThat(actualOutboxEvents).hasSize(1);
                })
                .as("There should be only one event per payment")
                .verifyComplete();

        assertThat(idempotencyCache.contains(paymentEvent.eventId())).isTrue();
    }

    @Test
    void shouldFailWhenMarkAsPaidOrderDoesNotExist() {
        PaymentEvent paymentEvent = getPaymentEvent(22222L);

        Mono<Void> processed = processingService.process(paymentEvent);

        StepVerifier.create(processed)
                .consumeErrorWith(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("Order not found with id: 22222");
                })
                .verify();
    }


    private PaymentEvent getPaymentEvent(Long orderId) {
        return new PaymentEvent(
                UUID.randomUUID(),
                orderId,
                1L,
                PaymentStatus.SUCCESS
        );
    }
}