package ru.nvkz.exception.handler;

import lombok.Getter;

public class DnsShopException extends RuntimeException {
    @Getter
    private final Object[] args;

    public DnsShopException(String key,  Object[] args) {
        super(key);
        this.args = args;
    }
}
