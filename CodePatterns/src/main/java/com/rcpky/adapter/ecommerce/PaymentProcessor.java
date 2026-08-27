package com.rcpky.adapter.ecommerce;

public interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
}
