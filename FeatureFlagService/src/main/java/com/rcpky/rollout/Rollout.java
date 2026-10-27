package com.rcpky.rollout;

import com.rcpky.flag.FlagValue;

import java.util.Objects;

public record Rollout(
        int percentage,
        FlagValue value,
        String bucketingKey
) {
    public Rollout {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100, got: " + percentage);
        }
        Objects.requireNonNull(value, "Rollout value must not be null");
    }
}
