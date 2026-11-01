package com.rcpky.service;

import com.rcpky.model.Reservation;

import java.time.Instant;
import java.util.List;

public interface IReservationService {
    Reservation reserve(String vehicleId, String chargePointId,
                        Instant scheduledStart, Instant scheduledEnd);
    Reservation cancelReservation(String reservationId);
    Reservation consumeReservation(String reservationId);
    List<Reservation> getReservations(String vehicleId);
}
