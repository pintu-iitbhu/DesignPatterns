package com.rcpky.exceptions;

public class InsufficientCashException extends RuntimeException {
    public InsufficientCashException(String message) {
        super(message);
    }
}
