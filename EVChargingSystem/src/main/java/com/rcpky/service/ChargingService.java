package com.rcpky.service;

import com.rcpky.enums.ChargePointStatus;
import com.rcpky.enums.ConnectorType;
import com.rcpky.enums.SessionStatus;
import com.rcpky.exception.ChargingException;
import com.rcpky.model.ChargePoint;
import com.rcpky.model.ChargingSession;
import com.rcpky.model.Vehicle;
import com.rcpky.observer.IChargingEventListener;
import com.rcpky.pricing.IPricingStrategy;
import com.rcpky.repository.IChargePointRepository;
import com.rcpky.repository.ISessionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChargingService implements IChargingService {

    private final IChargePointRepository chargePointRepository;
    private final ISessionRepository sessionRepository;
    private final List<IChargingEventListener> eventListeners;
    private final Map<String, IPricingStrategy> sessionPricingMap = new java.util.concurrent.ConcurrentHashMap<>();

    public ChargingService(IChargePointRepository chargePointRepository,
                           ISessionRepository sessionRepository,
                           List<IChargingEventListener> eventListeners) {
        this.chargePointRepository = chargePointRepository;
        this.sessionRepository = sessionRepository;
        this.eventListeners = eventListeners;
    }

    @Override
    public ChargingSession startSession(Vehicle vehicle, ChargePoint chargePoint,
                                        IPricingStrategy pricingStrategy) {
        if (!chargePoint.isAvailable() && chargePoint.getStatus() != ChargePointStatus.RESERVED) {
            throw new ChargingException(
                    "ChargePoint " + chargePoint.getId() + " is not available. Status: " + chargePoint.getStatus());
        }
        if (!chargePoint.canCharge(vehicle)) {
            throw new ChargingException(
                    "Connector mismatch: vehicle requires " + vehicle.getConnectorType()
                    + " but chargePoint offers " + chargePoint.getConnectorType());
        }

        String sessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ChargingSession session = new ChargingSession(
                sessionId, vehicle.getId(), chargePoint.getId(), pricingStrategy.getName());

        chargePoint.assignSession(sessionId);
        chargePointRepository.save(chargePoint);
        sessionRepository.save(session);
        sessionPricingMap.put(sessionId, pricingStrategy);

        eventListeners.forEach(l -> l.onSessionStarted(session));
        return session;
    }

    @Override
    public ChargingSession stopSession(String sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChargingException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ChargingException("Session " + sessionId + " is not active. Status: " + session.getStatus());
        }

        ChargePoint chargePoint = chargePointRepository.findById(session.getChargePointId())
                .orElseThrow(() -> new ChargingException("ChargePoint not found: " + session.getChargePointId()));

        long durationSeconds = session.getDurationSeconds();
        long durationMinutes = Math.max(1, durationSeconds / 60);
        double energyKwh = (durationSeconds / 3600.0) * chargePoint.getPowerLevel().getKilowatts();

        IPricingStrategy strategy = sessionPricingMap.getOrDefault(sessionId,
                new com.rcpky.pricing.FlatRatePricingStrategy(BigDecimal.valueOf(0.30)));
        BigDecimal cost = strategy.calculateCost(energyKwh, durationMinutes, chargePoint.getPowerLevel());
        sessionPricingMap.remove(sessionId);

        session.complete(energyKwh, cost);

        sessionRepository.save(session);
        chargePoint.clearSession();
        chargePointRepository.save(chargePoint);

        eventListeners.forEach(l -> l.onSessionEnded(session));
        return session;
    }

    @Override
    public Optional<ChargingSession> getActiveSession(String chargePointId) {
        return sessionRepository.findActiveByChargePointId(chargePointId);
    }

    @Override
    public List<ChargingSession> getSessionHistory(String vehicleId) {
        return sessionRepository.findByVehicleId(vehicleId);
    }

    @Override
    public List<ChargePoint> findAvailableChargePoints(String stationId, ConnectorType connectorType) {
        return chargePointRepository.findByStationId(stationId).stream()
                .filter(cp -> cp.isAvailable()
                        && (connectorType == null || cp.getConnectorType() == connectorType))
                .collect(Collectors.toList());
    }
}
