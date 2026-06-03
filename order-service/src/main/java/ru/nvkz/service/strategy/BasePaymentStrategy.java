package ru.nvkz.service.strategy;

import lombok.RequiredArgsConstructor;
import ru.nvkz.service.OrderService;


@RequiredArgsConstructor //DRY - дублирование кода
public abstract class BasePaymentStrategy implements PaymentStrategy {

    protected final OrderService orderService;
}
