package com.rcpky.model;

import com.rcpky.enums.ChargePointStatus;
import com.rcpky.enums.ConnectorType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChargingStation {
    private final String id;
    private final String name;
    private final String location;
    private final List<ChargePoint> chargePoints;

    public ChargingStation(String id, String name, String location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.chargePoints = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public List<ChargePoint> getChargePoints() { return chargePoints; }

    public void addChargePoint(ChargePoint chargePoint) {
        chargePoints.add(chargePoint);
    }

    public List<ChargePoint> getAvailableChargePoints(ConnectorType connectorType) {
        return chargePoints.stream()
                .filter(cp -> cp.getStatus() == ChargePointStatus.AVAILABLE
                        && cp.getConnectorType() == connectorType)
                .collect(Collectors.toList());
    }

    public List<ChargePoint> getAvailableChargePoints() {
        return chargePoints.stream()
                .filter(ChargePoint::isAvailable)
                .collect(Collectors.toList());
    }

    public int getTotalChargePoints() { return chargePoints.size(); }

    public int getAvailableCount() {
        return (int) chargePoints.stream()
                .filter(ChargePoint::isAvailable)
                .count();
    }

    @Override
    public String toString() {
        return String.format("ChargingStation{id='%s', name='%s', location='%s', chargePoints=%d, available=%d}",
                id, name, location, getTotalChargePoints(), getAvailableCount());
    }
}
