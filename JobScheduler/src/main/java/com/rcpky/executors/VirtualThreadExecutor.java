package com.rcpky.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * ITaskExecutor backed by Java 21 Virtual Threads (Project Loom).
 *
 * Why virtual threads for a job scheduler?
 *   Virtual threads are JVM-managed, not OS threads. They are extremely
 *   cheap to create (microseconds, ~1KB stack vs ~1MB for platform threads).
 *   When a virtual thread blocks (I/O, sleep, lock), the JVM parks it and
 *   reuses the underlying carrier (OS) thread for other work — zero OS blocking.
 *
 * Key difference from ThreadPoolTaskExecutor:
 *   ThreadPoolTaskExecutor   — fixed N OS threads; slow I/O-bound jobs occupy
 *                              a thread and starve other jobs if pool is full.
 *   VirtualThreadExecutor    — one virtual thread per task, no fixed limit.
 *                              10,000 concurrent I/O-bound jobs = 10,000
 *                              virtual threads but only ~handful of OS threads.
 *
 * When to prefer this:
 *   Jobs that do I/O (DB queries, HTTP calls, file reads). For pure CPU-bound
 *   jobs, ThreadPoolTaskExecutor with N = CPU cores is still better because
 *   virtual threads can't parallelise CPU work beyond physical cores.
 *
 * There is NO constructor `threads` parameter because virtual threads have
 * no pool size — the JVM creates one per submitted task automatically.
 */
public class VirtualThreadExecutor implements ITaskExecutor {

    // newVirtualThreadPerTaskExecutor: creates a new virtual thread for every
    // execute() call. No pre-allocated pool, no queuing, no backpressure.
    // Suitable when tasks are I/O-bound and blocking is expected.
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void execute(Runnable task) {
        pool.submit(task); // spawns a new virtual thread immediately
    }

    @Override
    public Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

    @Override
    public void shutdown() {
        // Same semantics as ThreadPoolExecutor.shutdown():
        // in-flight virtual threads finish; new submissions rejected.
        pool.shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
