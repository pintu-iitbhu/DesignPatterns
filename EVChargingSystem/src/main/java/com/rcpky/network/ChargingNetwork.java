package com.rcpky.network;

import com.rcpky.enums.ConnectorType;
import com.rcpky.model.ChargePoint;
import com.rcpky.model.ChargingStation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChargingNetwork {
    private static ChargingNetwork instance;
    private final Map<String, ChargingStation> stations = new LinkedHashMap<>();

    private ChargingNetwork() {}

    public static ChargingNetwork getInstance() {
        if (instance == null) instance = new ChargingNetwork();
        return instance;
    }

    public void addStation(ChargingStation station) {
        stations.put(station.getId(), station);
    }

    public Optional<ChargingStation> getStation(String stationId) {
        return Optional.ofNullable(stations.get(stationId));
    }

    public List<ChargingStation> getAllStations() {
        return new ArrayList<>(stations.values());
    }

    public List<ChargingStation> findStationsWithAvailableChargePoints(ConnectorType connectorType) {
        return stations.values().stream()
                .filter(s -> !s.getAvailableChargePoints(connectorType).isEmpty())
                .collect(Collectors.toList());
    }

    public void printNetworkStatus() {
        System.out.println("\n  ┌──────────────────────────────────────────────────────────┐");
        System.out.println("  │                    NETWORK STATUS                        │");
        System.out.println("  └──────────────────────────────────────────────────────────┘");
        for (ChargingStation station : stations.values()) {
            System.out.printf("  Station: %-20s | Location: %-15s | ChargePoints: %d / %d available%n",
                    station.getName(), station.getLocation(),
                    station.getAvailableCount(), station.getTotalChargePoints());
            for (ChargePoint cp : station.getChargePoints()) {
                System.out.printf("    ↳ ChargePoint %-10s | %-12s | %-14s | %s%n",
                        cp.getId(), cp.getConnectorType(), cp.getPowerLevel(), cp.getStatus());
            }
        }
        System.out.println();
    }
}
