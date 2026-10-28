package com.rcpky.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class Booking {
    private final String id;
    private final String userId;
    private final String holdId;
    private final String eventId;
    private final List<String> seatIds;
    private final double totalAmount;
    private final String idempotencyKey;
    private final Instant createdAt;
    private final AtomicReference<BookingStatus> status;
    private volatile Payment payment;
    private volatile List<Ticket> tickets;

    public Booking(String id, String userId, String holdId, String eventId,
                   List<String> seatIds, double totalAmount, String idempotencyKey) {
        this.id = id;
        this.userId = userId;
        this.holdId = holdId;
        this.eventId = eventId;
        this.seatIds = Collections.unmodifiableList(seatIds);
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.status = new AtomicReference<>(BookingStatus.PENDING);
    }

    public String getId()              { return id; }
    public String getUserId()          { return userId; }
    public String getHoldId()          { return holdId; }
    public String getEventId()         { return eventId; }
    public List<String> getSeatIds()   { return seatIds; }
    public double getTotalAmount()     { return totalAmount; }
    public String getIdempotencyKey()  { return idempotencyKey; }
    public Instant getCreatedAt()      { return createdAt; }
    public BookingStatus getStatus()   { return status.get(); }
    public Payment getPayment()        { return payment; }
    public List<Ticket> getTickets()   { return tickets; }

    public boolean transitionStatus(BookingStatus expected, BookingStatus next) {
        return status.compareAndSet(expected, next);
    }

    public void setPayment(Payment p)      { this.payment = p; }
    public void setTickets(List<Ticket> t) { this.tickets = Collections.unmodifiableList(t); }

    @Override
    public String toString() {
        return "Booking{id='" + id + "', userId='" + userId + "', event='" + eventId
               + "', seats=" + seatIds + ", amount=" + totalAmount + ", status=" + status.get() + "}";
    }
}
