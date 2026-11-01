package com.rcpky.model;

import com.rcpky.enums.ConnectorType;

public class Vehicle {
    private final String id;
    private final String ownerId;
    private final String name;
    private final ConnectorType connectorType;
    private final double batteryCapacityKwh;
    private double currentChargePercent;

    public Vehicle(String id, String ownerId, String name, ConnectorType connectorType,
                   double batteryCapacityKwh, double currentChargePercent) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.connectorType = connectorType;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.currentChargePercent = currentChargePercent;
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public ConnectorType getConnectorType() { return connectorType; }
    public double getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public double getCurrentChargePercent() { return currentChargePercent; }

    public void setCurrentChargePercent(double currentChargePercent) {
        this.currentChargePercent = Math.min(100.0, Math.max(0.0, currentChargePercent));
    }

    public double getRemainingCapacityKwh() {
        return batteryCapacityKwh * (1.0 - currentChargePercent / 100.0);
    }

    @Override
    public String toString() {
        return String.format("Vehicle{id='%s', name='%s', connector=%s, battery=%.1f kWh, charge=%.1f%%}",
                id, name, connectorType, batteryCapacityKwh, currentChargePercent);
    }
}
