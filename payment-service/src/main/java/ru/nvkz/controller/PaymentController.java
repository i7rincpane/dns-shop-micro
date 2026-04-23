package ru.nvkz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.Payment;
import ru.nvkz.dto.BankResponse;
import ru.nvkz.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /*
    PATCH/PUT: Означает - я знаю ресурс и хочу изменить его состояние.
    POST: Означает - я присылаю новое уведомление/событие, не знаю устройство БД, просто кидаю факт: Оплата свершилась.
    Почти все платежные шлюзы используют Webhook именно через POST. Это стандарт для уведомлений.
     */

    @PostMapping
    public Mono<Payment> handleWebhook(@RequestBody BankResponse bankResponse) {
        return paymentService.processWebhook(bankResponse);
    }
}
