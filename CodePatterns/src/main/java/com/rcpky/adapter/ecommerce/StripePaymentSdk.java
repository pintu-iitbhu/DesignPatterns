package com.rcpky.adapter.ecommerce;

public class StripePaymentSdk {

    public StripeResponse charge(
            String customerId,
            long amountInPaise,
            String currency
    ) {
        // External API call
        return new StripeResponse("transactionId", true, "stripe payment success");
    }
}
