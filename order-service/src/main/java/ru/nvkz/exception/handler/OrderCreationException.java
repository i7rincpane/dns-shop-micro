package ru.nvkz.exception.handler;

public class OrderCreationException extends RuntimeException {
    public OrderCreationException(String error) {
        super(error);
    }
}
