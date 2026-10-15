package com.rcpky.algo;

import com.rcpky.configs.SlidingWindowConfig;
import com.rcpky.models.RateLimitResult;
import com.rcpky.models.WindowState;
import com.rcpky.store.StateStore;

import java.util.concurrent.atomic.AtomicReference;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.ConcurrentMap;

public class SlidingWindowCounterAlgorithm implements RateLimitAlgorithm<SlidingWindowConfig> {

    private final StateStore<WindowState> store;
    // private final ConcurrentMap<String, WindowState> windows = new ConcurrentHashMap<>(); // Q2: unbounded heap growth

    public SlidingWindowCounterAlgorithm(StateStore<WindowState> store) {
        this.store = store;
    }

    @Override
    public RateLimitResult tryAcquire(String key, SlidingWindowConfig policy, long nowMillis) {
        // final RateLimitResult[] result = new RateLimitResult[1]; // Q3: array trick — workaround for lambda effectively-final constraint
        AtomicReference<RateLimitResult> result = new AtomicReference<>();

        store.compute(key, (ignored, oldState) -> {
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

                // result[0] = new RateLimitResult(true, remaining, 0);
                result.set(new RateLimitResult(true, remaining, 0));
            } else {
                long retryAfter = Math.max(
                        1,
                        policy.windowMillis() - elapsed
                );

                // result[0] = new RateLimitResult(false, 0, retryAfter);
                result.set(new RateLimitResult(
                        false,
                        0,
                        retryAfter
                ));
            }

            return new WindowState(
                    windowId,
                    previousCount,
                    currentCount
            );
        });

        // return result[0];
        return result.get();
    }
}
