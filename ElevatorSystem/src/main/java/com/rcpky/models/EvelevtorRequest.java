package com.rcpky.models;

import com.rcpky.Elevator;
import com.rcpky.enums.Direction;

public class EvelevtorRequest {
    int floorNo;
    Direction direction;

    public EvelevtorRequest(int floorNo, Direction direction) {
        this.floorNo = floorNo;
        this.direction = direction;
    }
}
