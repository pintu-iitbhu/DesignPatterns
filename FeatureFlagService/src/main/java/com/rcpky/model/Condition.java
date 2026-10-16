package com.rcpky.model;

public record Condition(
        String attribute,
        ConditionOperator operator,
        Object value
) {}
