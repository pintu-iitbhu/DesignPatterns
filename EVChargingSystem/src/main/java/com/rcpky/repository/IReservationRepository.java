package com.rcpky.repository;

import com.rcpky.model.Reservation;

import java.util.List;
import java.util.Optional;

public interface IReservationRepository {
    void save(Reservation reservation);
    Optional<Reservation> findById(String id);
    List<Reservation> findByVehicleId(String vehicleId);
    List<Reservation> findByChargePointId(String chargePointId);
    void delete(String id);
    List<Reservation> findAll();
}
