package com.rcpky.observer;

import com.rcpky.model.ChargingSession;

public class NotificationService implements IChargingEventListener {

    @Override
    public void onSessionStarted(ChargingSession session) {
        System.out.printf("  [NOTIFICATION] Session started: vehicle=%s at chargePoint=%s%n",
                session.getVehicleId(), session.getChargePointId());
    }

    @Override
    public void onSessionEnded(ChargingSession session) {
        System.out.printf("  [NOTIFICATION] Session ended: vehicle=%s | energy=%.3f kWh | cost=$%.2f%n",
                session.getVehicleId(), session.getEnergyDeliveredKwh(), session.getCost());
    }

    @Override
    public void onChargingFailed(String chargePointId, String reason) {
        System.out.printf("  [NOTIFICATION] Charging failed at chargePoint=%s: %s%n",
                chargePointId, reason);
    }
}
