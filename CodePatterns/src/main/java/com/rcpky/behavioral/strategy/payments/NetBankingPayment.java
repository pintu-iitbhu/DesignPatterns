package com.rcpky.behavioral.strategy.payments;

public class NetBankingPayment implements PaymentStrategy {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing payment of " + amount + " using Net Banking");
    }
}
