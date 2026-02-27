package com.innowise.orderservice.exception;

public class RemoteUserNotFoundException extends RuntimeException {
    public RemoteUserNotFoundException(String message) {
        super(message);
    }

  public RemoteUserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
