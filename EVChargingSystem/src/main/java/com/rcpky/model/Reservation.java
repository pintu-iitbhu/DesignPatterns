package com.rcpky.model;

import com.rcpky.enums.ReservationStatus;

import java.time.Instant;

public class Reservation {
    private final String id;
    private final String vehicleId;
    private final String chargePointId;
    private final Instant scheduledStart;
    private final Instant scheduledEnd;
    private final Instant createdAt;
    private ReservationStatus status;

    public Reservation(String id, String vehicleId, String chargePointId,
                       Instant scheduledStart, Instant scheduledEnd) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.chargePointId = chargePointId;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.createdAt = Instant.now();
        this.status = ReservationStatus.PENDING;
    }

    public String getId() { return id; }
    public String getVehicleId() { return vehicleId; }
    public String getChargePointId() { return chargePointId; }
    public Instant getScheduledStart() { return scheduledStart; }
    public Instant getScheduledEnd() { return scheduledEnd; }
    public Instant getCreatedAt() { return createdAt; }
    public ReservationStatus getStatus() { return status; }

    public void activate() { this.status = ReservationStatus.ACTIVE; }
    public void consume() { this.status = ReservationStatus.CONSUMED; }
    public void cancel() { this.status = ReservationStatus.CANCELLED; }
    public void expire() { this.status = ReservationStatus.EXPIRED; }

    @Override
    public String toString() {
        return String.format(
                "Reservation{id='%s', vehicle='%s', chargePoint='%s', start=%s, end=%s, status=%s}",
                id, vehicleId, chargePointId, scheduledStart, scheduledEnd, status);
    }
}
