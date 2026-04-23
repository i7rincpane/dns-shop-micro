package ru.nvkz.exception.handler;

public class NotFoundException extends DnsShopException {

    public NotFoundException(String key, Long id) {
        super(key, new Object[]{id});
    }
}