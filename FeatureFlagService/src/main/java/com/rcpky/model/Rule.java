package com.rcpky.model;

import java.util.List;

public record Rule(
        LogicOperator logicOperator,
        List<Condition> conditions,
        Object targetValue
) {}
