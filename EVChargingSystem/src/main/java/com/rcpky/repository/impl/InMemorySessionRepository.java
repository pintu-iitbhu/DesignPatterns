package com.rcpky.repository.impl;

import com.rcpky.enums.SessionStatus;
import com.rcpky.model.ChargingSession;
import com.rcpky.repository.ISessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemorySessionRepository implements ISessionRepository {
    private final Map<String, ChargingSession> store = new ConcurrentHashMap<>();

    @Override
    public void save(ChargingSession session) {
        store.put(session.getId(), session);
    }

    @Override
    public Optional<ChargingSession> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ChargingSession> findByVehicleId(String vehicleId) {
        return store.values().stream()
                .filter(s -> s.getVehicleId().equals(vehicleId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ChargingSession> findActiveByChargePointId(String chargePointId) {
        return store.values().stream()
                .filter(s -> s.getChargePointId().equals(chargePointId)
                        && s.getStatus() == SessionStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public List<ChargingSession> findAll() {
        return new ArrayList<>(store.values());
    }
}
