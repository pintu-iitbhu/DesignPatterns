package org.rcpky.clients;

import org.rcpky.strategy.IDriveStrategy;
import org.rcpky.strategy.SportsDriveStrategy;

public class SportsVehicle extends VehicleContext{
    public SportsVehicle() {
        super(new SportsDriveStrategy());
    }
}
