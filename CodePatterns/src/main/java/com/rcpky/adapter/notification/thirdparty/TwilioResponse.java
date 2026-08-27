package com.rcpky.adapter.notification.thirdparty;

public class TwilioResponse {
    private final String transactionId;
    private final boolean success;
    private final String message;

    public TwilioResponse(String transactionId, boolean success, String message) {
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
