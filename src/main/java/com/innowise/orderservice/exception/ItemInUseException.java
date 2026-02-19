package com.innowise.orderservice.exception;

public class ItemInUseException extends RuntimeException {
    public ItemInUseException(String message) {
        super(message);
    }

    public ItemInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
