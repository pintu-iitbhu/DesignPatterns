package com.rcpky.users;

import com.rcpky.parking_area.DisplayBoard;
import com.rcpky.parking_area.Entrance;
import com.rcpky.parking_area.Exit;
import com.rcpky.parking_spots.ParkingSpot;

public class Admin extends Account {
    public boolean addParkingSpot(ParkingSpot spot) { return true; }
    public boolean addDisplayBoard(DisplayBoard board) { return true; }
    public boolean addEntrance(Entrance entrance) { return true; }
    public boolean addExit(Exit exit) { return true; }
    public boolean resetPassword() { return true; }
}
