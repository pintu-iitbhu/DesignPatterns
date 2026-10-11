package com.rcpky;

import com.rcpky.enums.Direction;
import com.rcpky.strategy.DirectionBasedStrategy;
import com.rcpky.strategy.ElevatorSelectionStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElevatorSystem {
    private Map<Integer, Elevator> elevatorMap;
    private Map<Integer, Floor> floorMap;
    private ElevatorSelectionStrategy elevatorSelectionStrategy;

    private List<Elevator> init(){
        List<Elevator> elevators = new ArrayList<>();
        for(int i=1;i<=5;i++){
            Elevator elevator= new Elevator(i, 10);
            elevators.add(elevator);
            elevatorMap.put(i, elevator);
        }

        for (int i=1;i<=20;i++){
            floorMap.put(i, new Floor(i));
        }

        return elevators;

    }
    public  ElevatorSystem() {
        this.elevatorMap = new HashMap<Integer, Elevator>();
        this.floorMap = new HashMap<Integer, Floor>();
        elevatorSelectionStrategy= new DirectionBasedStrategy(init());
    }






}
