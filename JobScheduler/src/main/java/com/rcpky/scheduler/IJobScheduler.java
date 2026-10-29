package com.rcpky.scheduler;

/**
 * Public contract for the job scheduler.
 *
 * Returns a String job ID from every schedule method so callers can
 * cancel specific jobs later. Keeping the ID as a String (UUID) rather
 * than a typed handle keeps the interface simple and avoids exposing
 * internal Job objects to callers.
 */
public interface IJobScheduler {

    // Run exactly once after `delayMs` milliseconds.
    String scheduleOnce(Runnable task, long delayMs);

    // Run repeatedly. Next run is computed from the *scheduled* time —
    // maintains cadence even if execution takes time (see ReschedulingPolicy).
    String scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs);

    // Run repeatedly. Next run is computed from *end of last execution* —
    // guarantees a minimum gap between runs (see ReschedulingPolicy).
    String scheduleWithFixedDelay(Runnable task, long initialDelayMs, long delayMs);

    // Best-effort cancellation. Returns false if the job was already
    // completed, cancelled, or the ID is unknown.
    boolean cancel(String jobId);

    /**
     * Schedule a one-shot job with retry and lease semantics.
     *
     * @param task           work to execute
     * @param delayMs        initial delay before first attempt
     * @param maxRetries     how many additional attempts after the first failure (0 = none)
     * @param retryDelayMs   base backoff delay; actual delay = retryDelayMs * 2^attempt
     * @param leaseTimeoutMs max ms the task may run before it is forcibly cancelled (0 = unlimited)
     */
    String scheduleWithOptions(Runnable task, long delayMs,
                               int maxRetries, long retryDelayMs, long leaseTimeoutMs);

    // Graceful shutdown: stops accepting new jobs, waits for in-flight
    // tasks to complete, then terminates the worker pool.
    void shutdown();
}
