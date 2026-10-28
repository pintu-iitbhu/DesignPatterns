package com.rcpky.repository.impl;

import com.rcpky.model.Payment;
import com.rcpky.repository.IPaymentRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements IPaymentRepository {
    private final ConcurrentHashMap<String, Payment> byId             = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Payment> byIdempotencyKey = new ConcurrentHashMap<>();

    /**
     * Atomically registers idempotencyKey → payment; also indexes by paymentId on success.
     * Returns null if stored (insertion succeeded); returns existing if key was already taken.
     */
    @Override
    public Payment putIfAbsent(String idempotencyKey, Payment payment) {
        Payment existing = byIdempotencyKey.putIfAbsent(idempotencyKey, payment);
        if (existing == null) byId.put(payment.getId(), payment);
        return existing;
    }

    @Override public Optional<Payment> findById(String id)          { return Optional.ofNullable(byId.get(id)); }
    @Override public Optional<Payment> findByIdempotencyKey(String k){ return Optional.ofNullable(byIdempotencyKey.get(k)); }
}
