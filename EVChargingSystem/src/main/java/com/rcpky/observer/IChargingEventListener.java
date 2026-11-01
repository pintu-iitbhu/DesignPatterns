package com.rcpky.observer;

import com.rcpky.model.ChargingSession;

public interface IChargingEventListener {
    void onSessionStarted(ChargingSession session);
    void onSessionEnded(ChargingSession session);
    void onChargingFailed(String chargePointId, String reason);
}
