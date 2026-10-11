package com.rcpky;

import com.rcpky.enums.*;

import java.util.*;

public class Elevator {
    private int elvNo;
    private int capacity;
    private OperationalState operationalState;
    private DoorState doorState;
    private ElevatorState state;
    private Floor currentFloor;
    private Map<Integer, Floor> floorMap;


    public Elevator(int elvNo, int capacity) {
        this.elvNo = elvNo;
        this.capacity = capacity;
        this.doorState = DoorState.CLOSED;
        this.operationalState = OperationalState.OPERATIONAL;
        this.state = ElevatorState.IDLE;
        this.floorMap = new HashMap<>();
    }
}
