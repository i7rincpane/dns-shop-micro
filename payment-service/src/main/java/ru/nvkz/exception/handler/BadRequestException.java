package ru.nvkz.exception.handler;

public class BadRequestException extends DnsShopException {

    public BadRequestException(String key, Object[] args) {
        super(key, args);
    }
}