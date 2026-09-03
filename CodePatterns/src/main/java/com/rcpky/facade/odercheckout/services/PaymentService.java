package com.rcpky.facade.odercheckout.services;

import com.rcpky.adapter.ecommerce.PaymentResult;

import java.math.BigDecimal;

public class PaymentService {

    public PaymentResult processPayment(
            String customerId,
            BigDecimal amount
    ) {
        // Process payment
        return new PaymentResult(customerId,true,"Payment processed successfully");
    }

    public void refund(String paymentId) {
        // Refund payment
    }
}
