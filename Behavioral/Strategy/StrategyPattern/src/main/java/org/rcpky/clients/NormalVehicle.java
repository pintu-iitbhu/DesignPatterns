package org.rcpky.clients;

import org.rcpky.strategy.NormalDriveStrategy;

public class NormalVehicle extends VehicleContext{
    public NormalVehicle() {
        super(new NormalDriveStrategy());
    }
}
