package com.rcpky.behavioral.strategy.payments;

public class PaymentMain {

    public static void main(String[] args) {
        PaymentProcessor processor = new PaymentProcessor(new CreditCardPayment());
        processor.processPayment(1000);


        processor.setPaymentStrategy(
                new UPIPayment()
        );

        processor.processPayment(500);


        processor.setPaymentStrategy(new NetBankingPayment());

        processor.processPayment(2000);
    }
}
