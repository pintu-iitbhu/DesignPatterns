package com.rcpky.structural.adapter.ecommerce;

public interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
}
