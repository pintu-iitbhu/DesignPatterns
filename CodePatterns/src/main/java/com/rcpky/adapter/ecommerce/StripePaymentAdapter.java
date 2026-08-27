package com.rcpky.adapter.ecommerce;

public class StripePaymentAdapter implements PaymentProcessor {
    private StripePaymentSdk stripePaymentSdk;

    public StripePaymentAdapter(StripePaymentSdk stripePaymentSdk) {
        this.stripePaymentSdk = stripePaymentSdk;
    }

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        System.out.println("Processing payment with Stripe");
        StripeResponse response= stripePaymentSdk.charge(
                request.getCustomerId(),
                request.getAmount().longValue(),
                request.getCurrency()
        );

        System.out.println("Payment processed with Stripe");
        return PaymentResult.fromStripeResponse(response);

    }
}
