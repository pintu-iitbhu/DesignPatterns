package com.rcpky;

import com.rcpky.algo.RateLimitAlgorithm;
import com.rcpky.algo.TokenBucketAlgorithm;
import com.rcpky.configs.TokenBucketConfig;
import com.rcpky.http.RateLimitHeaders;
import com.rcpky.models.Bucket;
import com.rcpky.models.RateLimitResult;
import com.rcpky.store.InMemoryStateStore;
import com.rcpky.store.StateStore;

import java.time.Duration;

/**
 * Hello world!
 *RATE LIMITOR MACHINE CODING
 *
 * A rate limiter controls the number of requests a user can make to a service in a given timeframe.
 * Use a token bucket algorithm to allow bursts of requests while enforcing a limit over time.
 * Implement a sliding window counter to track requests in a specific time frame.
 * Consider using Redis for distributed rate limiting across multiple servers.
 * Example: Limit to 100 requests per minute per user.
 * Use HTTP headers to inform users of their current rate limit status.
 * code should be extensible to support different rate limiting algorithms.
 * code should be extensible to support different timeframes.
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        TokenBucketConfig config = new TokenBucketConfig(5, 1, 1_000);

        StateStore<Bucket> store = new InMemoryStateStore<>(Duration.ofHours(1));
        RateLimitAlgorithm<TokenBucketConfig> algorithm = new TokenBucketAlgorithm(store);

        RateLimiterService<TokenBucketConfig> service = new RateLimiterService<>(algorithm, config);

        String userId = "user-101";

        for (int i = 1; i <= 7; i++) {
            RateLimitResult result = service.allowRequest(userId);

            System.out.println(
                    "Request " + i
                            + " | Allowed: " + result.allowed()
                            + " | Remaining: " + result.remaining()
            );

            if (!result.allowed()) {
                System.out.println(RateLimitHeaders.from(result, config));
            }
        }
    }
}


/**
 * Functional requirements
 * 1. Limit requests per user.
 * 2. Support different algorithms.
 * 3. Support different time windows.
 * 4. Allow bursts when the selected algorithm supports them.
 * 5. Return the remaining request quota and reset time.
 * 6. Support configurable limits, such as 100 requests per minute.
 * 7. Make it easy to add new algorithms.
 *
 * Non-functional requirements
 * - Thread safety: Concurrent requests must not exceed the configured limit.
 * - Extensibility: New algorithms must not require changes to the main service.
 * - Performance: Request evaluation should be fast.
 * - Distributed support: Multiple application servers should share rate-limit state.
 * - Testability: Algorithms should be testable independently.
 *
 *
 *
 *
 */
