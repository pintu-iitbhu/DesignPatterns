package org.rcpky.clients;

import org.rcpky.strategy.IDriveStrategy;

public class VehicleContext {

    private IDriveStrategy strategy;

    public VehicleContext(IDriveStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(IDriveStrategy strategy) {
        this.strategy = strategy;
    }

    public void Drive() {
        strategy.Drive();
    }
}
