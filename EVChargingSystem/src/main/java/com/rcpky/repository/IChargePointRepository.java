package com.rcpky.repository;

import com.rcpky.enums.ChargePointStatus;
import com.rcpky.enums.ConnectorType;
import com.rcpky.model.ChargePoint;

import java.util.List;
import java.util.Optional;

public interface IChargePointRepository {
    void save(ChargePoint chargePoint);
    Optional<ChargePoint> findById(String id);
    List<ChargePoint> findByStationId(String stationId);
    List<ChargePoint> findByStatus(ChargePointStatus status);
    List<ChargePoint> findAvailable(ConnectorType connectorType);
    List<ChargePoint> findAll();
}
