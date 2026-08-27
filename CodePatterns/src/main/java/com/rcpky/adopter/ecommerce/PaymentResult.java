package com.rcpky.adopter.ecommerce;

public class PaymentResult {

    private String transactionId;
    private boolean success;
    private String message;

    public PaymentResult(String transactionId, boolean success, String message) {
        this.transactionId = transactionId;
        this.success = success;
        this.message = message;
    }

    public static PaymentResult fromStripeResponse(StripeResponse response) {

        return new PaymentResult(response.getTransactionId(), response.isSuccess(), response.getMessage());
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
