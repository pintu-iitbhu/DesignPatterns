package com.rcpky.enums;

public enum PowerLevel {
    L1(1.4),
    L2(7.2),
    DC_FAST_50(50.0),
    DC_FAST_150(150.0),
    DC_FAST_350(350.0);

    private final double kilowatts;

    PowerLevel(double kilowatts) {
        this.kilowatts = kilowatts;
    }

    public double getKilowatts() {
        return kilowatts;
    }
}
