package com.rcpky.rule;

public record Rule(
        String id,
        String name,
        int priority,
        RuleCondition condition,
        RuleAction action,
        boolean enabled
) {}
