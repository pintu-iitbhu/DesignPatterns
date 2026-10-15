package com.rcpky.algo;

import com.rcpky.configs.SlidingWindowConfig;
import com.rcpky.models.RateLimitResult;
import com.rcpky.models.WindowLog;
import com.rcpky.store.StateStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class SlidingWindowLogAlgorithm implements RateLimitAlgorithm<SlidingWindowConfig> {

    private final StateStore<WindowLog> store;

    public SlidingWindowLogAlgorithm(StateStore<WindowLog> store) {
        this.store = store;
    }

    @Override
    public RateLimitResult tryAcquire(String key, SlidingWindowConfig policy, long nowMillis) {
        AtomicReference<RateLimitResult> result = new AtomicReference<>();

        store.compute(key, (ignored, oldLog) -> {
            ArrayDeque<Long> deque = oldLog == null
                    ? new ArrayDeque<>()
                    : new ArrayDeque<>(oldLog.timestamps());

            long windowStart = nowMillis - policy.windowMillis();

            while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                deque.pollFirst();
            }

            if (deque.size() < policy.limit()) {
                deque.addLast(nowMillis);
                int remaining = policy.limit() - deque.size();
                result.set(new RateLimitResult(true, remaining, 0));
            } else {
                long oldestTimestamp = deque.peekFirst();
                long retryAfter = Math.max(1, oldestTimestamp + policy.windowMillis() - nowMillis);
                result.set(new RateLimitResult(false, 0, retryAfter));
            }

            return new WindowLog(new ArrayList<>(deque));
        });

        return result.get();
    }
}
