package ru.nvkz.service;

import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.nvkz.client.CartClient;
import ru.nvkz.client.ProductClient;
import ru.nvkz.domain.*;
import ru.nvkz.dto.CartItemDto;
import ru.nvkz.dto.OrderItemDto;
import ru.nvkz.dto.StockUpdateRequest;
import ru.nvkz.event.OrderCancelledEvent;
import ru.nvkz.event.OrderCreatedEvent;
import ru.nvkz.event.OrderPaidEvent;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.repository.OrderItemRepository;
import ru.nvkz.repository.OrderRepository;
import ru.nvkz.repository.OutboxRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartClient cartClient;
    private final ProductClient productClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    @Transactional
    public Mono<Order> markAsPaid(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> {

                    if (order.getStatus() == OrderStatus.PAID) {
                        return Mono.just(order);
                    }

                    order.setStatus(OrderStatus.PAID);

                    OrderPaidEvent payload = new OrderPaidEvent(orderId, order.getUserId());

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> outboxRepository.insert(getOutboxEvent(
                                            orderId,
                                            payload,
                                            OutboxEventType.ORDER_PAID))
                                    .thenReturn(savedOrder));
                });
    }

    @Transactional
    public Mono<Order> compensateOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> {

                    if (order.getStatus() == OrderStatus.CANCELLED) {
                        return Mono.just(order);
                    }

                    order.setStatus(OrderStatus.CANCELLED);
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                return orderItemRepository.findAllByOrderId(orderId)
                                        .collectList()
                                        .flatMap(orderItems -> {
                                            List<StockUpdateRequest> requests = orderItems.stream()
                                                    .map(orderItem -> new StockUpdateRequest(orderItem.getProductId(), orderItem.getQuantity()))
                                                    .toList();

                                            OrderCancelledEvent payload = new OrderCancelledEvent(
                                                    orderId,
                                                    order.getUserId(),
                                                    OrderCancelledEvent.Reason.PAYMENT_FAILED);

                                            return productClient.increase(requests)
                                                    .then(outboxRepository.insert(getOutboxEvent(
                                                            orderId,
                                                            payload,
                                                            OutboxEventType.ORDER_CANCELLED)))
                                                    .thenReturn(savedOrder);

                                        });
                            });
                });
    }

    @Transactional
    public Mono<Order> create(Long userId) {
        return cartClient.getCartByUserId(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Корзина для юзера не найдена", userId)))
                .flatMap(cartResponse -> {

                    List<CartItemDto> selectedItems = cartResponse.items();
                    selectedItems.removeIf(cartItemDto -> !cartItemDto.isSelected());

                    List<StockUpdateRequest> stockUpdatesRequest = mapToStockUpdateRequest(selectedItems);

                    return productClient.decrease(stockUpdatesRequest)
                            .then(orderRepository.save(new Order(
                                            null,
                                            userId,
                                            OrderStatus.NEW,
                                            cartResponse.totalSelectedPrice(),
                                            OffsetDateTime.now(),
                                            null
                                    )).flatMap(savedOrder -> {

                                                List<OrderItem> newOrderItems = selectedItems.stream()
                                                        .map(cartItemDto -> new OrderItem(
                                                                        null,
                                                                        savedOrder.getId(),
                                                                        cartItemDto.productId(),
                                                                        cartItemDto.productName(),
                                                                        cartItemDto.price(),
                                                                        cartItemDto.quantity()
                                                                )
                                                        ).toList();

                                                return orderItemRepository.saveAll(newOrderItems)
                                                        .then(Mono.just(savedOrder));

                                            }

                                    ).flatMap(savedOrder -> {

                                                var eventPayload = new OrderCreatedEvent(
                                                        savedOrder.getId(),
                                                        userId,
                                                        savedOrder.getTotalPrice(),
                                                        selectedItems.stream().map(cartItem -> new OrderItemDto(cartItem.productId(), cartItem.productName(), cartItem.price(), cartItem.quantity())).toList()
                                                );

                                                OutboxEvent outbox = getOutboxEvent(
                                                        savedOrder.getId(),
                                                        eventPayload,
                                                        OutboxEventType.ORDER_CREATED);

                                                return outboxRepository.insert(outbox)
                                                        .thenReturn(savedOrder);
                                            }

                                    ).onErrorResume(ex -> productClient.increase(stockUpdatesRequest)
                                            .then(Mono.error(ex)))
                            );

                });
    }

    private <T> OutboxEvent getOutboxEvent(Long aggregateId, T eventPayload, OutboxEventType type) {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateId(aggregateId.toString());
        outbox.setType(type);
        outbox.setPayload(Json.of(objectMapper.writeValueAsString(eventPayload)));
        return outbox;
    }

    private List<StockUpdateRequest> mapToStockUpdateRequest(List<CartItemDto> selectedItems) {
        return selectedItems.stream()
                .map(cartItemDto -> new StockUpdateRequest(cartItemDto.productId(), cartItemDto.quantity()))
                .toList();
    }

}