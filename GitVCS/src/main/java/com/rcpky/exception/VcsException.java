package com.rcpky.exception;

public class VcsException extends RuntimeException {
    public VcsException(String message) {
        super(message);
    }

    public VcsException(String message, Throwable cause) {
        super(message, cause);
    }
}
