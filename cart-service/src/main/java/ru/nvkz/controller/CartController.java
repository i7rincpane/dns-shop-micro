package ru.nvkz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.CartItem;
import ru.nvkz.dto.CartItemRequest;
import ru.nvkz.dto.CartItemUpdateDto;
import ru.nvkz.dto.CartResponse;
import ru.nvkz.service.CartService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Mono<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        return cartService.getCartByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<CartItem> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(userId, request.productId(), request.quantity());
    }

    @PatchMapping("/{productId}")
    public Mono<CartItem> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId,
            @RequestBody CartItemUpdateDto updateDto) {
        return cartService.updateItem(userId, productId, updateDto);
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        return cartService.clearCart(userId);
    }
}
