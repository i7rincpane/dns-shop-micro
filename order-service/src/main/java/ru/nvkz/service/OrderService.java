package ru.nvkz.service;

import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.nvkz.client.CartClient;
import ru.nvkz.client.ProductClient;
import ru.nvkz.domain.*;
import ru.nvkz.dto.CartItemDto;
import ru.nvkz.dto.OrderItemDto;
import ru.nvkz.dto.StockUpdateRequest;
import ru.nvkz.dto.OrderCreatedEvent;
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
    private final R2dbcEntityTemplate template;

    @Transactional
    public Mono<Order> create(Long userId) {
        return cartClient.getCartByUserId(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Корзина для юзера не найдена", userId)))
                .flatMap(cartResponse -> {

                    List<CartItemDto> selectedItems = cartResponse.items()
                            .stream()
                            .filter(CartItemDto::isSelected)
                            .toList();


                    if (selectedItems.isEmpty()) {
                        return Mono.error(new RuntimeException("Не возможно создать заказ с пустой корзиной"));
                    }

                    List<StockUpdateRequest> stockRequest = selectedItems.stream()
                            .map(cartItemDto -> new StockUpdateRequest(cartItemDto.productId(), cartItemDto.quantity()))
                            .toList();

                    return productClient.decrease(stockRequest)
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
                                                        selectedItems.stream().map(cartItem -> new OrderItemDto(cartItem.productId(), cartItem.productName(), cartItem.price(), cartItem.quantity())).toList()
                                                );

                                                OutboxEvent outbox = new OutboxEvent();
                                                outbox.setId(UUID.randomUUID());
                                                outbox.setAggregateId(savedOrder.getId().toString());
                                                outbox.setType(OutboxEventType.ORDER_CREATED);
                                                outbox.setPayload(Json.of(objectMapper.writeValueAsString(eventPayload)));

                                                return template.insert(outbox)
                                                        .thenReturn(savedOrder);
                                            }

                                    ).onErrorResume(ex -> productClient.increase(stockRequest)
                                            .then(Mono.error(ex)))
                            );

                });
    }
}