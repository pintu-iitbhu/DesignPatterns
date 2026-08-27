package com.rcpky.adopter.ecommerce;

public class StripeResponse {

    private String transactionId;
    private boolean success;
    private String message;

    public StripeResponse(String transactionId, boolean success, String message) {
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
