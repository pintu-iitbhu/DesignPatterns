package com.rcpky.repository.impl;

import com.rcpky.enums.ChargePointStatus;
import com.rcpky.enums.ConnectorType;
import com.rcpky.model.ChargePoint;
import com.rcpky.repository.IChargePointRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryChargePointRepository implements IChargePointRepository {
    private final Map<String, ChargePoint> store = new ConcurrentHashMap<>();

    @Override
    public void save(ChargePoint chargePoint) {
        store.put(chargePoint.getId(), chargePoint);
    }

    @Override
    public Optional<ChargePoint> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ChargePoint> findByStationId(String stationId) {
        return store.values().stream()
                .filter(cp -> cp.getStationId().equals(stationId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargePoint> findByStatus(ChargePointStatus status) {
        return store.values().stream()
                .filter(cp -> cp.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargePoint> findAvailable(ConnectorType connectorType) {
        return store.values().stream()
                .filter(cp -> cp.isAvailable() && cp.getConnectorType() == connectorType)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChargePoint> findAll() {
        return new ArrayList<>(store.values());
    }
}
