package com.rcpky.exception;

public class TicketingException extends RuntimeException {
    public TicketingException(String message) { super(message); }
    public TicketingException(String message, Throwable cause) { super(message, cause); }
}
