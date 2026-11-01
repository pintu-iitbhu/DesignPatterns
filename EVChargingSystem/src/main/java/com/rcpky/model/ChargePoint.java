package com.rcpky.model;

import com.rcpky.enums.ChargePointStatus;
import com.rcpky.enums.ConnectorType;
import com.rcpky.enums.PowerLevel;

public class ChargePoint {
    private final String id;
    private final String stationId;
    private final ConnectorType connectorType;
    private final PowerLevel powerLevel;
    private ChargePointStatus status;
    private String currentSessionId;

    public ChargePoint(String id, String stationId, ConnectorType connectorType, PowerLevel powerLevel) {
        this.id = id;
        this.stationId = stationId;
        this.connectorType = connectorType;
        this.powerLevel = powerLevel;
        this.status = ChargePointStatus.AVAILABLE;
    }

    public String getId() { return id; }
    public String getStationId() { return stationId; }
    public ConnectorType getConnectorType() { return connectorType; }
    public PowerLevel getPowerLevel() { return powerLevel; }
    public ChargePointStatus getStatus() { return status; }
    public String getCurrentSessionId() { return currentSessionId; }

    public boolean isAvailable() {
        return status == ChargePointStatus.AVAILABLE;
    }

    public boolean canCharge(Vehicle vehicle) {
        return connectorType == vehicle.getConnectorType();
    }

    public void assignSession(String sessionId) {
        this.currentSessionId = sessionId;
        this.status = ChargePointStatus.CHARGING;
    }

    public void reserve() {
        this.status = ChargePointStatus.RESERVED;
    }

    public void clearSession() {
        this.currentSessionId = null;
        this.status = ChargePointStatus.AVAILABLE;
    }

    public void markFaulted() {
        this.status = ChargePointStatus.FAULTED;
        this.currentSessionId = null;
    }

    public void markOutOfService() {
        this.status = ChargePointStatus.OUT_OF_SERVICE;
        this.currentSessionId = null;
    }

    @Override
    public String toString() {
        return String.format("ChargePoint{id='%s', connector=%s, power=%.1f kW, status=%s}",
                id, connectorType, powerLevel.getKilowatts(), status);
    }
}
