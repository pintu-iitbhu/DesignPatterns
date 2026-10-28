package com.rcpky.repository;

import com.rcpky.model.Payment;

import java.util.Optional;

public interface IPaymentRepository {
    /**
     * Atomically stores payment under idempotencyKey if absent AND saves by paymentId.
     * Returns null if insertion succeeded; returns the existing Payment if key was already present.
     */
    Payment putIfAbsent(String idempotencyKey, Payment payment);
    Optional<Payment> findById(String paymentId);
    Optional<Payment> findByIdempotencyKey(String key);
}
