package com.rcpky;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SlidingWindowCounterAlgorithm
        implements RateLimitAlgorithm {

    private record WindowState(
            long windowId,
            long previousCount,
            long currentCount
    ) {}

    private final ConcurrentMap<String, WindowState> windows =
            new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryAcquire(
            String key,
            RateLimitConfig config,
            long nowMillis
    ) {
        if (!(config instanceof SlidingWindowConfig policy)) {
            throw new IllegalArgumentException("SlidingWindowConfig required");
        }

        final RateLimitResult[] result = new RateLimitResult[1];

        windows.compute(key, (ignored, oldState) -> {
            long windowId = nowMillis / policy.windowMillis();

            long previousCount = 0;
            long currentCount = 0;

            if (oldState != null) {
                if (oldState.windowId() == windowId) {
                    previousCount = oldState.previousCount();
                    currentCount = oldState.currentCount();
                } else if (oldState.windowId() == windowId - 1) {
                    previousCount = oldState.currentCount();
                }
            }

            long windowStart = windowId * policy.windowMillis();
            long elapsed = nowMillis - windowStart;

            double previousWeight =
                    1.0 - (double) elapsed / policy.windowMillis();

            double estimatedCount =
                    currentCount + previousCount * previousWeight;

            if (estimatedCount + 1 <= policy.limit()) {
                currentCount++;

                double updatedCount =
                        currentCount + previousCount * previousWeight;

                int remaining = Math.max(
                        0,
                        (int) Math.floor(policy.limit() - updatedCount)
                );

                result[0] = new RateLimitResult(true, remaining, 0);
            } else {
                long retryAfter = Math.max(
                        1,
                        policy.windowMillis() - elapsed
                );

                result[0] = new RateLimitResult(
                        false,
                        0,
                        retryAfter
                );
            }

            return new WindowState(
                    windowId,
                    previousCount,
                    currentCount
            );
        });

        return result[0];
    }
}
