package com.rcpky.store;

import com.rcpky.models.Job;

/**
 * Abstraction over job storage and timed retrieval.
 *
 * Why separate this from JobScheduler?
 *   Single Responsibility: JobScheduler should only coordinate dispatch.
 *   Storage concerns (ordering, locking, registry) belong here.
 *   Dependency Inversion: JobScheduler depends on this interface, not a
 *   concrete PriorityQueue. Swap implementations (e.g. Redis-backed,
 *   persistent DB, distributed queue) without touching the scheduler.
 */
public interface IJobQueue {

    // Add a job to the queue. Notifies the dispatcher if it is waiting.
    void enqueue(Job job);

    /**
     * Blocks until the next job is due (nextRunTime <= now), then returns it.
     * Returns null if the queue is shut down.
     * Throws InterruptedException on thread interrupt.
     *
     * This design keeps ALL timing and synchronisation logic inside the
     * implementation, making JobScheduler.dispatch() a simple 8-line loop.
     */
    Job awaitNext() throws InterruptedException;

    // Remove a job from the registry by ID (used by cancel and job completion).
    // Returns the removed Job, or null if not found.
    Job remove(String jobId);

    // Check if a job is still registered (used by reschedule to detect cancellation).
    boolean contains(String jobId);

    // Signal shutdown so awaitNext() unblocks and returns null.
    void shutdown();
}
