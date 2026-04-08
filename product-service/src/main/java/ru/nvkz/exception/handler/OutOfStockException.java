package ru.nvkz.exception.handler;

public class OutOfStockException extends DnsShopException {
    public OutOfStockException(String key, Long id, Integer qty) {
        super(key, new Object[]{id, qty});
    }
}
