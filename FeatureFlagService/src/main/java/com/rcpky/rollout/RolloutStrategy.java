package com.rcpky.rollout;

import com.rcpky.flag.FlagEvaluator;
import com.rcpky.flag.FlagValue;

import java.util.Optional;

/**
 * Strategy for determining whether a given user/key falls within a rollout.
 *
 * <p>The default implementation ({@link RolloutEvaluator}) uses deterministic
 * CRC32 bucketing. Alternative strategies (e.g. Murmur3, geography-based,
 * time-gated) can be injected into {@link FlagEvaluator} without modifying it.
 */
@FunctionalInterface
public interface RolloutStrategy {
    Optional<FlagValue> evaluate(String flagName, String bucketKey, Rollout rollout);
}
