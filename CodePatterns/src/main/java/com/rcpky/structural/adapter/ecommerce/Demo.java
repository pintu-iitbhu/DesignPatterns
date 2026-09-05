package com.rcpky.structural.adapter.ecommerce;

import java.math.BigDecimal;

public class Demo {

    public static void main(String[] args) {

        StripePaymentSdk stripePaymentSdk = new StripePaymentSdk();
        PaymentProcessor paymentProcessor = new StripePaymentAdapter(stripePaymentSdk);

        PaymentRequest paymentRequest = new PaymentRequest("customerId", new BigDecimal(100), "INR");
        PaymentResult paymentResult = paymentProcessor.processPayment(paymentRequest);

        System.out.println(paymentResult.getMessage());

    }
}
