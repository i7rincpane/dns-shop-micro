package ru.nvkz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.nvkz.client.ProductClient;
import ru.nvkz.domain.CartItem;
import ru.nvkz.dto.CartItemDto;
import ru.nvkz.dto.CartItemUpdateDto;
import ru.nvkz.dto.CartResponse;
import ru.nvkz.dto.ProductFullResponse;
import ru.nvkz.exception.handler.NotFoundException;
import ru.nvkz.mapper.CartItemMapper;
import ru.nvkz.repository.CartRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final CartItemMapper cartItemMapper;


    @Transactional
    public Mono<CartItem> updateItem(Long userId, Long productId, CartItemUpdateDto updateDto) {
        return cartRepository.findByUserIdAndProductId(userId, productId)
                .switchIfEmpty(Mono.error(new NotFoundException("error.product.notfound", productId)))
                .flatMap(cartItem -> {

                    if (updateDto.quantity() != null && updateDto.quantity() <= 0) {
                        return cartRepository.delete(cartItem)
                                .then(Mono.empty());
                    }

                    cartItemMapper.updateCartItemFromDto(updateDto, cartItem);
                    return cartRepository.save(cartItem);
                })
                .retryWhen(Retry.max(3).filter(ex -> ex instanceof OptimisticLockingFailureException));
    }

    //POST потому что клиент говорит - "Я хочу новый товар", по сути идет создание товара.
    //защищен не полностью при дабл клиеке, может переключить счетчик два раза(не критично), но второй товар не добавит из за уникального констрейнта
    //по хорошему можно одним запросом проверять констренйт и в случае ошибки обновлять счетчик.
    @Transactional
    public Mono<CartItem> addItem(Long userId, Long productId, int quantity) {
        return productClient.getProductAllById(List.of(productId))
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(new NotFoundException("error.product.notfound", productId)))
                .flatMap(product ->
                        cartRepository.findByUserIdAndProductId(userId, productId)
                                .flatMap(cartItem -> {
                                    cartItem.setQuantity(cartItem.getQuantity() + quantity);
                                    return cartRepository.save(cartItem);
                                }))
                .switchIfEmpty(Mono.defer(() -> cartRepository.save(new CartItem(null, userId, productId, quantity, true, null))));
    }

    public Mono<CartResponse> getCartByUserId(Long id) {
        return cartRepository.findByUserId(id)
                .collectList()
                .flatMap(items -> {

                    if (items.isEmpty()) {
                        return Mono.just(new CartResponse(List.of(), BigDecimal.ZERO));
                    }

                    List<Long> ids = items.stream()
                            .map(CartItem::getProductId)
                            .toList();
                    return productClient.getProductAllById(ids)
                            .collectMap(ProductFullResponse::id)
                            .map(productMap -> buildCartResponse(items, productMap));
                });
    }

    public Mono<Void> clearCart(Long userId) {
        return cartRepository.deleteAllByUserId(userId);
    }

    private CartResponse buildCartResponse(List<CartItem> items, Map<Long, ProductFullResponse> productMap) {
        List<CartItemDto> cartItemDtos = items.stream()
                .filter(cartItem -> {
                    boolean exists = productMap.containsKey(cartItem.getProductId());
                    if (!exists) {
                        log.warn("Product {} found in cart but missing in Product-Service!", cartItem.getProductId());
                    }
                    return exists;
                })
                .map(cartItem -> {
                    ProductFullResponse product = productMap.get(cartItem.getProductId());
                    return new CartItemDto(cartItem.getProductId(),
                            product.fullName(),
                            product.price(),
                            cartItem.getQuantity(),
                            cartItem.isSelected()
                    );
                })
                .toList();
        return new CartResponse(cartItemDtos, cartItemDtos.stream()
                .filter(CartItemDto::isSelected)
                .map(CartItemDto::subTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

}