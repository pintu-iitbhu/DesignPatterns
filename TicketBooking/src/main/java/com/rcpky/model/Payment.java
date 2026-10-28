package com.rcpky.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class Payment {
    private final String id;
    private final String bookingId;
    private final String userId;
    private final double amount;
    private final String idempotencyKey;
    private final AtomicReference<PaymentStatus> status;
    private volatile Instant processedAt;

    public Payment(String id, String bookingId, String userId, double amount, String idempotencyKey) {
        this.id = id;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = new AtomicReference<>(PaymentStatus.PENDING);
    }

    public String getId()              { return id; }
    public String getBookingId()       { return bookingId; }
    public String getUserId()          { return userId; }
    public double getAmount()          { return amount; }
    public String getIdempotencyKey()  { return idempotencyKey; }
    public PaymentStatus getStatus()   { return status.get(); }
    public Instant getProcessedAt()    { return processedAt; }

    public boolean transitionStatus(PaymentStatus expected, PaymentStatus next) {
        boolean ok = status.compareAndSet(expected, next);
        if (ok && next != PaymentStatus.PENDING) processedAt = Instant.now();
        return ok;
    }

    @Override
    public String toString() {
        return "Payment{id='" + id + "', bookingId='" + bookingId
               + "', amount=" + amount + ", status=" + status.get() + "}";
    }
}
