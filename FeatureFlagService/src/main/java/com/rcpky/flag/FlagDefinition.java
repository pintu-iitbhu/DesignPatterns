package com.rcpky.flag;

import com.rcpky.rollout.Rollout;
import com.rcpky.rule.Rule;

import java.util.List;
import java.util.Objects;

public record FlagDefinition(
        String name,
        FlagType type,
        FlagValue defaultValue,
        List<Rule> rules,
        Rollout rollout,
        boolean isActive
) {
    public FlagDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Flag name must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null for flag: " + name);
        Objects.requireNonNull(defaultValue, "defaultValue must not be null for flag: " + name);
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
