package com.rcpky.payments;

import com.rcpky.enums.PaymentStatus;

public class CreditCard extends Payment {
    public CreditCard(double amt) { super(amt); }
    public boolean initiateTransaction() {
        status = PaymentStatus.COMPLETED;
        System.out.println("Credit card payment of $" + amount + " completed.");
        return true;
    }
}
