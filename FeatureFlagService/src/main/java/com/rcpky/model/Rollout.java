package com.rcpky.model;

public record Rollout(
        int percentage,
        Object value
) {
    public Rollout {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100, got: " + percentage);
        }
    }
}
