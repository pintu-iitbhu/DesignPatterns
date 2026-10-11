package com.rcpky.strategy;

import com.rcpky.Elevator;
import com.rcpky.Floor;

import java.util.List;

public interface ElevatorSelectionStrategy {
    Elevator getElevator(Floor rqFloor);
}
