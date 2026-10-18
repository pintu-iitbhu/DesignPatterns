package com.rcpky.core;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class ColumnDefinition {

    private final String name;
    private final ColumnType type;
    private final Set<Constraint> constraints;

    public ColumnDefinition(String name, ColumnType type, Constraint... constraints) {
        this.name = name;
        this.type = type;
        this.constraints = constraints.length > 0
                ? EnumSet.copyOf(Arrays.asList(constraints))
                : EnumSet.noneOf(Constraint.class);
        // PRIMARY_KEY implies NOT_NULL + UNIQUE
        if (this.constraints.contains(Constraint.PRIMARY_KEY)) {
            this.constraints.add(Constraint.NOT_NULL);
            this.constraints.add(Constraint.UNIQUE);
        }
    }

    public String getName() { return name; }
    public ColumnType getType() { return type; }
    public Set<Constraint> getConstraints() { return constraints; }

    public boolean hasConstraint(Constraint c) {
        return constraints.contains(c);
    }

    @Override
    public String toString() {
        return name + " " + type + " " + constraints;
    }
}
