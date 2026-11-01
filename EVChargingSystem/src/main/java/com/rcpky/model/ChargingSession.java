package com.rcpky.model;

import com.rcpky.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class ChargingSession {
    private final String id;
    private final String vehicleId;
    private final String chargePointId;
    private final Instant startTime;
    private Instant endTime;
    private double energyDeliveredKwh;
    private BigDecimal cost;
    private SessionStatus status;
    private String pricingStrategyName;

    public ChargingSession(String id, String vehicleId, String chargePointId,
                           String pricingStrategyName) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.chargePointId = chargePointId;
        this.startTime = Instant.now();
        this.status = SessionStatus.ACTIVE;
        this.pricingStrategyName = pricingStrategyName;
        this.energyDeliveredKwh = 0.0;
        this.cost = BigDecimal.ZERO;
    }

    public String getId() { return id; }
    public String getVehicleId() { return vehicleId; }
    public String getChargePointId() { return chargePointId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public double getEnergyDeliveredKwh() { return energyDeliveredKwh; }
    public BigDecimal getCost() { return cost; }
    public SessionStatus getStatus() { return status; }
    public String getPricingStrategyName() { return pricingStrategyName; }

    public long getDurationMinutes() {
        Instant end = (endTime != null) ? endTime : Instant.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    public long getDurationSeconds() {
        Instant end = (endTime != null) ? endTime : Instant.now();
        return java.time.Duration.between(startTime, end).toSeconds();
    }

    public void complete(double energyDeliveredKwh, BigDecimal cost) {
        this.endTime = Instant.now();
        this.energyDeliveredKwh = energyDeliveredKwh;
        this.cost = cost;
        this.status = SessionStatus.COMPLETED;
    }

    public void cancel() {
        this.endTime = Instant.now();
        this.status = SessionStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return String.format(
                "ChargingSession{id='%s', vehicle='%s', chargePoint='%s', energy=%.3f kWh, cost=$%.2f, status=%s, pricing=%s}",
                id, vehicleId, chargePointId, energyDeliveredKwh,
                cost, status, pricingStrategyName);
    }
}
