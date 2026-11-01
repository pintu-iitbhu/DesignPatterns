package com.rcpky.repository;

import com.rcpky.model.ChargingSession;

import java.util.List;
import java.util.Optional;

public interface ISessionRepository {
    void save(ChargingSession session);
    Optional<ChargingSession> findById(String id);
    List<ChargingSession> findByVehicleId(String vehicleId);
    Optional<ChargingSession> findActiveByChargePointId(String chargePointId);
    List<ChargingSession> findAll();
}
