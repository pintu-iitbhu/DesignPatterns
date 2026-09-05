package com.rcpky.payments;

import com.rcpky.enums.PaymentStatus;

public class Cash extends Payment {
    public Cash(double amt) { super(amt); }
    public boolean initiateTransaction() {
        status = PaymentStatus.COMPLETED;
        System.out.println("com.rcpky.payments.Cash payment of $" + amount + " completed.");
        return true;
    }
}
