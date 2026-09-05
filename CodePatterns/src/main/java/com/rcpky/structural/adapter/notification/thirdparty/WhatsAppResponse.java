package com.rcpky.structural.adapter.notification.thirdparty;

public class WhatsAppResponse {
    private final String transactionId;
    private final boolean success;
    private final String message;

    public WhatsAppResponse(String transactionId, boolean success, String message) {
        this.transactionId = transactionId;
        this.success = success;
        this.message = message;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
