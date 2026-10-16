package com.rcpky.engine;

import com.rcpky.model.Rollout;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.CRC32;

public class RolloutEvaluator {

    /**
     * Sticky bucketing: hash(flagName + "|" + userId) % 100 < percentage.
     * CRC32 is deterministic across JVM restarts, guaranteeing stickiness.
     * Bucketing key includes the flag name so each flag rolls out independently.
     */
    public Optional<Object> evaluate(String flagName, String userId, Rollout rollout) {
        if (rollout == null || userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        int bucket = bucket(flagName, userId);
        if (bucket < rollout.percentage()) {
            return Optional.of(rollout.value());
        }
        return Optional.empty();
    }

    private int bucket(String flagName, String userId) {
        String key = flagName + "|" + userId;
        CRC32 crc32 = new CRC32();
        crc32.update(key.getBytes(StandardCharsets.UTF_8));
        return (int) (crc32.getValue() % 100);
    }
}
