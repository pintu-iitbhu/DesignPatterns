package org.rcpky.clients;


import org.rcpky.strategy.NormalDriveStrategy;
import org.rcpky.strategy.XYZDriveStrategy;

public class OffRoadVehicle extends VehicleContext{
    public OffRoadVehicle() {
        super(new XYZDriveStrategy());
    }
}
