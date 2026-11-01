package com.rcpky;

import com.rcpky.enums.ChargePointStatus;
import com.rcpky.enums.ConnectorType;
import com.rcpky.enums.PowerLevel;
import com.rcpky.enums.ReservationStatus;
import com.rcpky.enums.SessionStatus;
import com.rcpky.exception.ChargingException;
import com.rcpky.model.ChargePoint;
import com.rcpky.model.ChargingSession;
import com.rcpky.model.Reservation;
import com.rcpky.model.Vehicle;
import com.rcpky.observer.NotificationService;
import com.rcpky.pricing.FlatRatePricingStrategy;
import com.rcpky.pricing.IPricingStrategy;
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

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ChargingSystemTest {

    private IChargingService chargingService;
    private IReservationService reservationService;
    private IChargePointRepository chargePointRepo;
    private ISessionRepository sessionRepo;

    private ChargePoint ccsChargePoint;
    private ChargePoint type2ChargePoint;
    private Vehicle ccsVehicle;
    private Vehicle type2Vehicle;
    private IPricingStrategy flatRate;
    private IPricingStrategy timeBased;

    @Before
    public void setUp() {
        chargePointRepo = new InMemoryChargePointRepository();
        sessionRepo = new InMemorySessionRepository();
        IReservationRepository reservationRepo = new InMemoryReservationRepository();

        chargingService = new ChargingService(chargePointRepo, sessionRepo,
                Arrays.asList(new NotificationService()));
        reservationService = new ReservationService(reservationRepo, chargePointRepo);

        ccsChargePoint = new ChargePoint("CP-T1", "S-TEST", ConnectorType.CCS, PowerLevel.DC_FAST_50);
        type2ChargePoint = new ChargePoint("CP-T2", "S-TEST", ConnectorType.TYPE2, PowerLevel.L2);
        chargePointRepo.save(ccsChargePoint);
        chargePointRepo.save(type2ChargePoint);

        ccsVehicle = new Vehicle("V-T1", "owner-1", "Test CCS EV",
                ConnectorType.CCS, 40.0, 30.0);
        type2Vehicle = new Vehicle("V-T2", "owner-2", "Test Type2 EV",
                ConnectorType.TYPE2, 60.0, 50.0);

        flatRate = new FlatRatePricingStrategy(new BigDecimal("0.30"));
        timeBased = new TimeBasedPricingStrategy(new BigDecimal("0.10"));
    }

    // ─── startSession ───────────────────────────────────────────────────────────

    @Test
    public void testStartSession_success() {
        ChargingSession session = chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);

        assertNotNull(session);
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
        assertEquals(ccsVehicle.getId(), session.getVehicleId());
        assertEquals(ccsChargePoint.getId(), session.getChargePointId());
        assertEquals(ChargePointStatus.CHARGING, ccsChargePoint.getStatus());
        assertEquals(session.getId(), ccsChargePoint.getCurrentSessionId());
    }

    @Test(expected = ChargingException.class)
    public void testStartSession_connectorMismatch_throwsException() {
        chargingService.startSession(type2Vehicle, ccsChargePoint, flatRate);
    }

    @Test(expected = ChargingException.class)
    public void testStartSession_chargePointAlreadyInUse_throwsException() {
        chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        Vehicle anotherCCS = new Vehicle("V-T3", "owner-3", "Another CCS EV",
                ConnectorType.CCS, 40.0, 20.0);
        chargingService.startSession(anotherCCS, ccsChargePoint, flatRate);
    }

    // ─── stopSession ────────────────────────────────────────────────────────────

    @Test
    public void testStopSession_success() throws InterruptedException {
        ChargingSession session = chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        Thread.sleep(500);
        ChargingSession completed = chargingService.stopSession(session.getId());

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getEndTime());
        assertTrue(completed.getEnergyDeliveredKwh() >= 0);
        assertEquals(ChargePointStatus.AVAILABLE, ccsChargePoint.getStatus());
        assertNull(ccsChargePoint.getCurrentSessionId());
    }

    @Test(expected = ChargingException.class)
    public void testStopSession_nonExistent_throwsException() {
        chargingService.stopSession("NONEXISTENT-SESSION");
    }

    @Test(expected = ChargingException.class)
    public void testStopSession_alreadyStopped_throwsException() {
        ChargingSession session = chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        chargingService.stopSession(session.getId());
        chargingService.stopSession(session.getId());
    }

    // ─── getActiveSession ───────────────────────────────────────────────────────

    @Test
    public void testGetActiveSession_returnsActiveSession() {
        ChargingSession session = chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        assertTrue(chargingService.getActiveSession(ccsChargePoint.getId()).isPresent());
        assertEquals(session.getId(),
                chargingService.getActiveSession(ccsChargePoint.getId()).get().getId());
    }

    @Test
    public void testGetActiveSession_noActiveSession_returnsEmpty() {
        assertTrue(chargingService.getActiveSession(type2ChargePoint.getId()).isEmpty());
    }

    // ─── findAvailableChargePoints ──────────────────────────────────────────────

    @Test
    public void testFindAvailableChargePoints_returnsCorrectOnes() {
        List<ChargePoint> available = chargingService.findAvailableChargePoints("S-TEST", ConnectorType.CCS);
        assertEquals(1, available.size());
        assertEquals("CP-T1", available.get(0).getId());
    }

    @Test
    public void testFindAvailableChargePoints_afterSessionStart_reducesCount() {
        chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        List<ChargePoint> available = chargingService.findAvailableChargePoints("S-TEST", ConnectorType.CCS);
        assertEquals(0, available.size());
    }

    // ─── getSessionHistory ──────────────────────────────────────────────────────

    @Test
    public void testGetSessionHistory_returnsAllSessions() throws InterruptedException {
        ChargingSession s1 = chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        chargingService.stopSession(s1.getId());

        ChargingSession s2 = chargingService.startSession(ccsVehicle, ccsChargePoint, timeBased);
        chargingService.stopSession(s2.getId());

        List<ChargingSession> history = chargingService.getSessionHistory(ccsVehicle.getId());
        assertEquals(2, history.size());
    }

    // ─── Reservation ────────────────────────────────────────────────────────────

    @Test
    public void testReservation_success() {
        Reservation r = reservationService.reserve(
                ccsVehicle.getId(), "CP-T1",
                Instant.now(), Instant.now().plusSeconds(3600));

        assertNotNull(r);
        assertEquals(ReservationStatus.ACTIVE, r.getStatus());
        assertEquals(ChargePointStatus.RESERVED, ccsChargePoint.getStatus());
    }

    @Test(expected = ChargingException.class)
    public void testReservation_onBusyChargePoint_throwsException() {
        chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        reservationService.reserve(
                type2Vehicle.getId(), "CP-T1",
                Instant.now(), Instant.now().plusSeconds(3600));
    }

    @Test
    public void testCancelReservation_restoresChargePointAvailability() {
        Reservation r = reservationService.reserve(
                ccsVehicle.getId(), "CP-T1",
                Instant.now(), Instant.now().plusSeconds(3600));

        reservationService.cancelReservation(r.getId());
        assertEquals(ReservationStatus.CANCELLED, r.getStatus());
        assertEquals(ChargePointStatus.AVAILABLE, ccsChargePoint.getStatus());
    }

    @Test
    public void testConsumeReservation_thenStartSession() {
        Reservation r = reservationService.reserve(
                ccsVehicle.getId(), "CP-T1",
                Instant.now(), Instant.now().plusSeconds(3600));

        reservationService.consumeReservation(r.getId());
        assertEquals(ReservationStatus.CONSUMED, r.getStatus());
        assertEquals(ChargePointStatus.AVAILABLE, ccsChargePoint.getStatus());

        ChargingSession session = chargingService.startSession(ccsVehicle, ccsChargePoint, flatRate);
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
    }

    // ─── Pricing ─────────────────────────────────────────────────────────────────

    @Test
    public void testFlatRatePricing_calculatesCorrectCost() {
        BigDecimal cost = flatRate.calculateCost(10.0, 30, PowerLevel.DC_FAST_50);
        assertEquals(new BigDecimal("3.00"), cost);
    }

    @Test
    public void testTimeBasedPricing_calculatesCorrectCost() {
        BigDecimal cost = timeBased.calculateCost(5.0, 45, PowerLevel.L2);
        assertEquals(new BigDecimal("4.50"), cost);
    }

    @Test
    public void testTimeBasedPricing_minimumOneMinute() {
        BigDecimal cost = timeBased.calculateCost(0.001, 0, PowerLevel.L2);
        assertEquals(new BigDecimal("0.10"), cost);
    }
}
