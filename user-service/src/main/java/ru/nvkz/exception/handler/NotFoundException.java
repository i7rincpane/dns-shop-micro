package ru.nvkz.exception.handler;

import lombok.Getter;

public class NotFoundException extends RuntimeException {
    @Getter
    private final Object[] args;

    public NotFoundException(String key, Long id) {
        super(key);
        this.args = new Object[]{id};
    }
}