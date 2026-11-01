package com.rcpky.service;

import com.rcpky.enums.ReservationStatus;
import com.rcpky.exception.ChargingException;
import com.rcpky.model.ChargePoint;
import com.rcpky.model.Reservation;
import com.rcpky.repository.IChargePointRepository;
import com.rcpky.repository.IReservationRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReservationService implements IReservationService {

    private final IReservationRepository reservationRepository;
    private final IChargePointRepository chargePointRepository;

    public ReservationService(IReservationRepository reservationRepository,
                              IChargePointRepository chargePointRepository) {
        this.reservationRepository = reservationRepository;
        this.chargePointRepository = chargePointRepository;
    }

    @Override
    public Reservation reserve(String vehicleId, String chargePointId,
                               Instant scheduledStart, Instant scheduledEnd) {
        ChargePoint chargePoint = chargePointRepository.findById(chargePointId)
                .orElseThrow(() -> new ChargingException("ChargePoint not found: " + chargePointId));

        if (!chargePoint.isAvailable()) {
            throw new ChargingException(
                    "ChargePoint " + chargePointId + " is not available for reservation. Status: "
                    + chargePoint.getStatus());
        }

        if (!scheduledEnd.isAfter(scheduledStart)) {
            throw new ChargingException("scheduledEnd must be after scheduledStart");
        }

        String reservationId = "RSV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Reservation reservation = new Reservation(
                reservationId, vehicleId, chargePointId, scheduledStart, scheduledEnd);
        reservation.activate();

        chargePoint.reserve();
        chargePointRepository.save(chargePoint);
        reservationRepository.save(reservation);

        System.out.printf("  [RESERVATION] %s reserved chargePoint=%s from %s to %s%n",
                reservationId, chargePointId, scheduledStart, scheduledEnd);
        return reservation;
    }

    @Override
    public Reservation cancelReservation(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ChargingException("Reservation not found: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CONSUMED
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ChargingException("Cannot cancel reservation in state: " + reservation.getStatus());
        }

        ChargePoint chargePoint = chargePointRepository.findById(reservation.getChargePointId())
                .orElseThrow(() -> new ChargingException("ChargePoint not found"));

        reservation.cancel();
        chargePoint.clearSession();
        reservationRepository.save(reservation);
        chargePointRepository.save(chargePoint);

        System.out.printf("  [RESERVATION] %s cancelled%n", reservationId);
        return reservation;
    }

    @Override
    public Reservation consumeReservation(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ChargingException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ChargingException("Reservation " + reservationId + " is not active");
        }

        ChargePoint chargePoint = chargePointRepository.findById(reservation.getChargePointId())
                .orElseThrow(() -> new ChargingException("ChargePoint not found"));

        reservation.consume();
        chargePoint.clearSession();
        reservationRepository.save(reservation);
        chargePointRepository.save(chargePoint);

        System.out.printf("  [RESERVATION] %s consumed — chargePoint now available for session%n", reservationId);
        return reservation;
    }

    @Override
    public List<Reservation> getReservations(String vehicleId) {
        return reservationRepository.findByVehicleId(vehicleId);
    }
}
