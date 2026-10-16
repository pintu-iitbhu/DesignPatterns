package com.rcpky.exception;

public class FlagNotFoundException extends RuntimeException {
    public FlagNotFoundException(String flagName) {
        super("Flag not found: " + flagName);
    }
}
