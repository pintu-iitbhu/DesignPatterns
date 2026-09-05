package com.rcpky.parking_spots;

import com.rcpky.vehicles.Vehicle;

public class Large extends ParkingSpot {
    public Large(int id) { super(id); }
    public boolean assignVehicle(Vehicle v) {
        if (isFree) {
            System.out.println("Allocated com.rcpky.parkingspots.Large slot " + id + " to " + v.getLicenseNo());
            this.vehicle = v; isFree = false; return true;
        }
        return false;
    }
}
