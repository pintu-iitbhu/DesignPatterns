package com.rcpky.rule;

public record Condition(
        String attribute,
        ConditionOperator operator,
        Object value
) {}
