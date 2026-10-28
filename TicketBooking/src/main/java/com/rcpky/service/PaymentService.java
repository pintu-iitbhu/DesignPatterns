package com.rcpky.service;

import com.rcpky.exception.PaymentFailedException;
import com.rcpky.model.Payment;
import com.rcpky.model.PaymentStatus;
import com.rcpky.repository.IPaymentRepository;

import java.util.UUID;

/**
 * Payment processor with idempotency-key deduplication.
 *
 * SOLID applied:
 *  S — only processes and refunds payments; storage delegated to IPaymentRepository.
 *  D — depends on IPaymentRepository (abstraction, not ConcurrentHashMap directly).
 *
 * Idempotency contract:
 *  - Same key + SUCCESS  → returns cached payment (no double charge).
 *  - Same key + FAILED   → re-throws PaymentFailedException (consistent result).
 *  - Same key + REFUNDED → returns cached refunded payment.
 */
public class PaymentService implements IPaymentService {

    private final IPaymentRepository paymentRepository;
    private final double failureRate;

    public PaymentService(IPaymentRepository paymentRepository) {
        this(paymentRepository, 0.0);
    }

    public PaymentService(IPaymentRepository paymentRepository, double failureRate) {
        this.paymentRepository = paymentRepository;
        this.failureRate = failureRate;
    }

    @Override
    public Payment processPayment(String userId, String bookingId,
                                   double amount, String idempotencyKey) {
        Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == PaymentStatus.FAILED)
                throw new PaymentFailedException("Payment already failed for key: " + idempotencyKey);
            return existing;
        }

        String paymentId = "pay-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Payment payment  = new Payment(paymentId, bookingId, userId, amount, idempotencyKey);

        Payment stored = paymentRepository.putIfAbsent(idempotencyKey, payment);
        if (stored != null) {
            if (stored.getStatus() == PaymentStatus.FAILED)
                throw new PaymentFailedException("Payment already failed for key: " + idempotencyKey);
            return stored;
        }

        boolean success = Math.random() > failureRate;
        if (success) {
            payment.transitionStatus(PaymentStatus.PENDING, PaymentStatus.SUCCESS);
            return payment;
        } else {
            payment.transitionStatus(PaymentStatus.PENDING, PaymentStatus.FAILED);
            throw new PaymentFailedException("Payment gateway declined charge for booking: " + bookingId);
        }
    }

    @Override
    public Payment refundPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentFailedException("Payment not found: " + paymentId));
        payment.transitionStatus(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);
        return payment;
    }
}
