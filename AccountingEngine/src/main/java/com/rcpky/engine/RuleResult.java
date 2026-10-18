package com.rcpky.engine;

public record RuleResult(
        String ruleId,
        String ruleName,
        boolean conditionMet,
        String actionType,
        int scoreDelta,
        String message
) {}
