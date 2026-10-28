package com.rcpky.service;

import com.rcpky.model.Payment;

public interface IPaymentService {
    Payment processPayment(String userId, String bookingId, double amount, String idempotencyKey);
    Payment refundPayment(String paymentId);
}
