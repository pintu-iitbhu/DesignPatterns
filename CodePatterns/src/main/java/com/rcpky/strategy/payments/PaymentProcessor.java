package com.rcpky.strategy.payments;

public class PaymentProcessor {
    private PaymentStrategy paymentStrategy;

    public PaymentProcessor(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void processPayment(double amount) {

        if (paymentStrategy == null) {
            throw new IllegalStateException(
                    "Payment strategy is not configured"
            );
        }

        paymentStrategy.processPayment(amount);
    }
}
