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

    // Graceful shutdown: stops accepting new jobs, waits for in-flight
    // tasks to complete, then terminates the worker pool.
    void shutdown();
}
