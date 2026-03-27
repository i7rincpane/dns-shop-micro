package ru.nvkz.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.nvkz.client.ProductClient;
import ru.nvkz.domain.CartItem;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.mapper.CartItemMapper;
import ru.nvkz.repository.CartRepository;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private CartItemMapper cartItemMapper;
    @InjectMocks
    private CartService cartService;

    private final Long userId = 1L;
    private final Long productId = 100L;
    private final Integer quantity = 2;


    @Test
    void shouldThrowNotWoundWhenProductDoesNotExist() {
        when(productClient.getProductAllById(anyList())).thenReturn(Flux.empty());

        Mono<CartItem> result = cartService.addItem(userId, productId, quantity);

        StepVerifier.create(result)
                .expectError(NotFoundException.class)
                .verify();
    }

}
