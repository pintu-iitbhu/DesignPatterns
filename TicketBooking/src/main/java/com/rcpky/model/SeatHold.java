package com.rcpky.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SeatHold {
    private final String id;
    private final String userId;
    private final String eventId;
    private final List<String> seatIds;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final AtomicBoolean active;

    public SeatHold(String id, String userId, String eventId, List<String> seatIds, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.seatIds = Collections.unmodifiableList(seatIds);
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.active = new AtomicBoolean(true);
    }

    public String getId()              { return id; }
    public String getUserId()          { return userId; }
    public String getEventId()         { return eventId; }
    public List<String> getSeatIds()   { return seatIds; }
    public Instant getCreatedAt()      { return createdAt; }
    public Instant getExpiresAt()      { return expiresAt; }
    public boolean isActive()          { return active.get(); }

    /** CAS true→false; returns true only for the caller that wins the deactivation. */
    public boolean deactivate()        { return active.compareAndSet(true, false); }

    public boolean isExpired()         { return Instant.now().isAfter(expiresAt); }

    @Override
    public String toString() {
        return "SeatHold{id='" + id + "', userId='" + userId + "', seats=" + seatIds
               + ", expires=" + expiresAt + ", active=" + active.get() + "}";
    }
}
