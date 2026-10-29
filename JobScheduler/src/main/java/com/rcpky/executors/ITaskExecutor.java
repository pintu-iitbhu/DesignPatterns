package com.rcpky.executors;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Dependency Inversion: JobScheduler depends on this abstraction, not on
 * ThreadPoolExecutor directly. This allows swapping the execution backend:
 *
 *   ThreadPoolTaskExecutor  — fixed-size thread pool (current default)
 *   VirtualThreadExecutor   — Java 21 virtual threads (one per task, zero OS threads blocked)
 *   SyncTaskExecutor        — runs tasks on the caller thread (useful for testing)
 *
 * Without this interface, every change to execution strategy requires
 * modifying JobScheduler itself, violating the Open/Closed Principle.
 */
public interface ITaskExecutor {

    // Submit a task for asynchronous execution. Must not block the caller.
    void execute(Runnable task);

    // Submit a task and return a Future for result/timeout tracking.
    // Used by the lease executor to enforce per-job execution deadlines.
    Future<?> submit(Runnable task);

    // Initiate orderly shutdown. No new tasks accepted; in-flight tasks complete.
    void shutdown();

    // Block until all in-flight tasks finish or the timeout elapses.
    // Called by JobScheduler.shutdown() to ensure clean termination.
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
