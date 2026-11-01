package com.rcpky.service;

import com.rcpky.enums.ConnectorType;
import com.rcpky.model.ChargePoint;
import com.rcpky.model.ChargingSession;
import com.rcpky.model.Vehicle;
import com.rcpky.pricing.IPricingStrategy;

import java.util.List;
import java.util.Optional;

public interface IChargingService {
    ChargingSession startSession(Vehicle vehicle, ChargePoint chargePoint, IPricingStrategy pricingStrategy);
    ChargingSession stopSession(String sessionId);
    Optional<ChargingSession> getActiveSession(String chargePointId);
    List<ChargingSession> getSessionHistory(String vehicleId);
    List<ChargePoint> findAvailableChargePoints(String stationId, ConnectorType connectorType);
}
