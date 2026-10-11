package com.rcpky.strategy;

import com.rcpky.Elevator;
import com.rcpky.Floor;
import com.rcpky.enums.Direction;

import java.util.ArrayList;
import java.util.List;

public class DirectionBasedStrategy implements ElevatorSelectionStrategy {

    private List<Elevator> elevatorList;

    public DirectionBasedStrategy(List<Elevator> elevatorList) {
        this.elevatorList = elevatorList;
    }


    @Override
    public Elevator getElevator(Floor rqFloor) {
        List<Elevator> listOfElvMovingInSameDirection = new ArrayList<>();

        for (Elevator elevator : elevatorList) {
            if (elevator.getState().equals(rqFloor.getDirection()) || elevator.getState().equals(Direction.IDLE)) {
               listOfElvMovingInSameDirection.add(elevator);
            }
        }

        final Elevator[] result = {null};
        final Integer[] minVal = {Integer.MAX_VALUE};
        listOfElvMovingInSameDirection.stream().forEach(elevator -> {
            int diff= elevator.getCurrentFloor().getFloorNo() - rqFloor.getFloorNo();
            if (Math.abs(diff)< minVal[0]) {
                minVal[0] =diff;
                result[0] =elevator;
            }
        });

        return result[0];
    }
}
