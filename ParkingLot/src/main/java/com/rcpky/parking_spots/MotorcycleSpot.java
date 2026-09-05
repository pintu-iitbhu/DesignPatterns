package com.rcpky.parking_spots;

import com.rcpky.vehicles.Vehicle;

public class MotorcycleSpot extends ParkingSpot {
    public MotorcycleSpot(int id) { super(id); }
    public boolean assignVehicle(Vehicle v) {
        if (isFree) {
            System.out.println("Allocated com.rcpky.vehicles.Motorcycle slot " + id + " to " + v.getLicenseNo());
            this.vehicle = v; isFree = false; return true;
        }
        return false;
    }
}
