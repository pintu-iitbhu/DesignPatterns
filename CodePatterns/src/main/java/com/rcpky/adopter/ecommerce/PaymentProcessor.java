package com.rcpky.adopter.ecommerce;

public interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
}
