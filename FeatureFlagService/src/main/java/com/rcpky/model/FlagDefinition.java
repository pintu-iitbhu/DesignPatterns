package com.rcpky.model;

import java.util.List;

public record FlagDefinition(
        String name,
        FlagType type,
        Object defaultValue,
        List<Rule> rules,
        Rollout rollout,
        boolean isActive
) {
    public FlagDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Flag name must not be blank");
        }
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
