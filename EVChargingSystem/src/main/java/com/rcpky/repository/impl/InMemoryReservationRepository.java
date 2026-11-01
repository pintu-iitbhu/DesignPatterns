package com.rcpky.repository.impl;

import com.rcpky.model.Reservation;
import com.rcpky.repository.IReservationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryReservationRepository implements IReservationRepository {
    private final Map<String, Reservation> store = new ConcurrentHashMap<>();

    @Override
    public void save(Reservation reservation) {
        store.put(reservation.getId(), reservation);
    }

    @Override
    public Optional<Reservation> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Reservation> findByVehicleId(String vehicleId) {
        return store.values().stream()
                .filter(r -> r.getVehicleId().equals(vehicleId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByChargePointId(String chargePointId) {
        return store.values().stream()
                .filter(r -> r.getChargePointId().equals(chargePointId))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public List<Reservation> findAll() {
        return new ArrayList<>(store.values());
    }
}
