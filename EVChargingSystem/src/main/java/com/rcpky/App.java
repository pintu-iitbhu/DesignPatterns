package com.rcpky;

import com.rcpky.enums.ConnectorType;
import com.rcpky.enums.PowerLevel;
import com.rcpky.exception.ChargingException;
import com.rcpky.model.ChargePoint;
import com.rcpky.model.ChargingSession;
import com.rcpky.model.ChargingStation;
import com.rcpky.model.Reservation;
import com.rcpky.model.Vehicle;
import com.rcpky.network.ChargingNetwork;
import com.rcpky.observer.IChargingEventListener;
import com.rcpky.observer.NotificationService;
import com.rcpky.pricing.FlatRatePricingStrategy;
import com.rcpky.pricing.IPricingStrategy;
import com.rcpky.pricing.PeakOffPeakPricingStrategy;
import com.rcpky.pricing.TimeBasedPricingStrategy;
import com.rcpky.repository.IChargePointRepository;
import com.rcpky.repository.IReservationRepository;
import com.rcpky.repository.ISessionRepository;
import com.rcpky.repository.impl.InMemoryChargePointRepository;
import com.rcpky.repository.impl.InMemoryReservationRepository;
import com.rcpky.repository.impl.InMemorySessionRepository;
import com.rcpky.service.ChargingService;
import com.rcpky.service.IChargingService;
import com.rcpky.service.IReservationService;
import com.rcpky.service.ReservationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              EV CHARGING SYSTEM — DEMO                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // ─── INFRASTRUCTURE SETUP ───────────────────────────────────────────────
        IChargePointRepository chargePointRepo   = new InMemoryChargePointRepository();
        ISessionRepository     sessionRepo       = new InMemorySessionRepository();
        IReservationRepository reservationRepo   = new InMemoryReservationRepository();

        List<IChargingEventListener> listeners   = Arrays.asList(new NotificationService());

        IChargingService    chargingService     = new ChargingService(chargePointRepo, sessionRepo, listeners);
        IReservationService reservationService  = new ReservationService(reservationRepo, chargePointRepo);

        IPricingStrategy flatRate    = new FlatRatePricingStrategy(new BigDecimal("0.30"));
        IPricingStrategy timeBased   = new TimeBasedPricingStrategy(new BigDecimal("0.10"));
        IPricingStrategy peakOffPeak = new PeakOffPeakPricingStrategy(
                                            new BigDecimal("0.45"), new BigDecimal("0.20"));

        // ─── STATION & CHARGEPOINT SETUP ────────────────────────────────────────
        ChargingStation station1 = new ChargingStation("S1", "GreenCharge Downtown", "MG Road, Bengaluru");
        ChargePoint cp1 = new ChargePoint("CP-01", "S1", ConnectorType.CCS,     PowerLevel.DC_FAST_150);
        ChargePoint cp2 = new ChargePoint("CP-02", "S1", ConnectorType.TYPE2,   PowerLevel.L2);
        ChargePoint cp3 = new ChargePoint("CP-03", "S1", ConnectorType.CHADEMO, PowerLevel.DC_FAST_50);
        station1.addChargePoint(cp1);
        station1.addChargePoint(cp2);
        station1.addChargePoint(cp3);
        chargePointRepo.save(cp1);
        chargePointRepo.save(cp2);
        chargePointRepo.save(cp3);

        ChargingStation station2 = new ChargingStation("S2", "QuickCharge Highway", "NH-44, Pune");
        ChargePoint cp4 = new ChargePoint("CP-04", "S2", ConnectorType.CCS,   PowerLevel.DC_FAST_350);
        ChargePoint cp5 = new ChargePoint("CP-05", "S2", ConnectorType.TESLA, PowerLevel.DC_FAST_150);
        station2.addChargePoint(cp4);
        station2.addChargePoint(cp5);
        chargePointRepo.save(cp4);
        chargePointRepo.save(cp5);

        ChargingNetwork network = ChargingNetwork.getInstance();
        network.addStation(station1);
        network.addStation(station2);

        // ─── VEHICLES ────────────────────────────────────────────────────────────
        Vehicle tataEV  = new Vehicle("V1", "owner-1", "Tata Nexon EV",  ConnectorType.CCS,     40.5, 20.0);
        Vehicle bmwEV   = new Vehicle("V2", "owner-2", "BMW iX",         ConnectorType.TYPE2,   76.6, 55.0);
        Vehicle leafEV  = new Vehicle("V3", "owner-3", "Nissan Leaf",    ConnectorType.CHADEMO, 40.0, 30.0);
        Vehicle teslaEV = new Vehicle("V4", "owner-4", "Tesla Model 3",  ConnectorType.TESLA,   82.0, 10.0);
        Vehicle wrongEV = new Vehicle("V5", "owner-5", "Hyundai Ioniq",  ConnectorType.J1772,   77.4, 25.0);
        Vehicle kiaEV   = new Vehicle("V6", "owner-6", "Kia EV6",        ConnectorType.CCS,     77.4, 40.0);

        network.printNetworkStatus();

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 1: Basic charging session with flat-rate pricing
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 1: Basic Charging Session (Flat Rate $0.30/kWh) ━━━");
        System.out.println("→ " + tataEV);

        List<ChargePoint> available = chargingService.findAvailableChargePoints("S1", ConnectorType.CCS);
        System.out.println("→ Available CCS chargePoints at S1: " + available.size());

        ChargingSession s1 = chargingService.startSession(tataEV, cp1, flatRate);
        System.out.println("→ Session started: " + s1.getId() + " | CP-01 status: " + cp1.getStatus());
        Thread.sleep(2000);

        ChargingSession c1 = chargingService.stopSession(s1.getId());
        System.out.printf("→ Completed | Energy: %.4f kWh | Duration: %ds | Cost: $%.2f%n%n",
                c1.getEnergyDeliveredKwh(), c1.getDurationSeconds(), c1.getCost());

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 2: Time-based pricing
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 2: Time-Based Pricing Session ($0.10/min) ━━━");
        System.out.println("→ " + bmwEV);

        ChargingSession s2 = chargingService.startSession(bmwEV, cp2, timeBased);
        Thread.sleep(1500);
        ChargingSession c2 = chargingService.stopSession(s2.getId());
        System.out.printf("→ Completed | Energy: %.4f kWh | Duration: %ds | Cost: $%.2f%n%n",
                c2.getEnergyDeliveredKwh(), c2.getDurationSeconds(), c2.getCost());

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 3: Peak/Off-Peak pricing
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 3: Peak/Off-Peak Pricing (peak=$0.45, off-peak=$0.20/kWh) ━━━");
        System.out.println("→ " + leafEV);

        ChargingSession s3 = chargingService.startSession(leafEV, cp3, peakOffPeak);
        Thread.sleep(1000);
        ChargingSession c3 = chargingService.stopSession(s3.getId());
        System.out.printf("→ Completed | Energy: %.4f kWh | Duration: %ds | Cost: $%.2f | Strategy: %s%n%n",
                c3.getEnergyDeliveredKwh(), c3.getDurationSeconds(), c3.getCost(), c3.getPricingStrategyName());

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 4: Reservation → consume → start session
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 4: Reserve a ChargePoint, then Start Session ━━━");
        System.out.println("→ " + teslaEV);

        Reservation r1 = reservationService.reserve(
                teslaEV.getId(), "CP-05", Instant.now(), Instant.now().plusSeconds(3600));
        System.out.println("→ Reserved: " + r1.getId() + " | CP-05 status: " + cp5.getStatus());

        Reservation consumed = reservationService.consumeReservation(r1.getId());
        System.out.println("→ Reservation consumed: " + consumed.getStatus() + " | CP-05 status: " + cp5.getStatus());

        ChargingSession s4 = chargingService.startSession(teslaEV, cp5, flatRate);
        Thread.sleep(1000);
        ChargingSession c4 = chargingService.stopSession(s4.getId());
        System.out.printf("→ Completed | Energy: %.4f kWh | Cost: $%.2f%n%n",
                c4.getEnergyDeliveredKwh(), c4.getCost());

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 5: Cancel a reservation → chargePoint back to AVAILABLE
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 5: Cancel a Reservation ━━━");

        Reservation r2 = reservationService.reserve(
                kiaEV.getId(), "CP-04", Instant.now(), Instant.now().plusSeconds(1800));
        System.out.println("→ Reserved: " + r2.getId() + " | CP-04 status: " + cp4.getStatus());

        reservationService.cancelReservation(r2.getId());
        System.out.println("→ CP-04 status after cancel: " + cp4.getStatus() + "\n");

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 6: Connector type mismatch → ChargingException
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 6: Connector Mismatch → ChargingException ━━━");
        System.out.println("→ J1772 vehicle trying CCS chargePoint CP-04");
        try {
            chargingService.startSession(wrongEV, cp4, flatRate);
        } catch (ChargingException e) {
            System.out.println("→ [EXPECTED ERROR] " + e.getMessage() + "\n");
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 7: ChargePoint already in use → ChargingException
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 7: ChargePoint Already In Use → ChargingException ━━━");

        ChargingSession s7 = chargingService.startSession(tataEV, cp1, flatRate);
        System.out.println("→ Session started: " + s7.getId() + " | CP-01 status: " + cp1.getStatus());
        try {
            chargingService.startSession(kiaEV, cp1, flatRate);
        } catch (ChargingException e) {
            System.out.println("→ [EXPECTED ERROR] " + e.getMessage());
        }
        chargingService.stopSession(s7.getId());
        System.out.println("→ Session stopped | CP-01 status: " + cp1.getStatus() + "\n");

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 8: Network discovery — find stations with available CCS
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 8: Network Discovery — Stations with Available CCS ━━━");
        network.findStationsWithAvailableChargePoints(ConnectorType.CCS)
               .forEach(s -> System.out.println("  → " + s));
        System.out.println();

        // ─── SESSION HISTORY ─────────────────────────────────────────────────
        System.out.println("━━━ SESSION HISTORY — Tata Nexon EV (V1) ━━━");
        chargingService.getSessionHistory(tataEV.getId())
                       .forEach(s -> System.out.println("  " + s));

        network.printNetworkStatus();

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                   END OF DEMONSTRATION                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
}
