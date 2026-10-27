package com.rcpky.executors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Concrete ITaskExecutor backed by a fixed-size ThreadPoolExecutor.
 *
 * Why fixed core == max pool size (no elasticity)?
 *   Predictable resource usage. A job scheduler typically has a known
 *   concurrency budget (e.g. 4 threads). Elastic pools spike under load
 *   and can cause OOM. Fixed size provides backpressure instead.
 *
 * Why LinkedBlockingQueue (unbounded work queue)?
 *   If all worker threads are busy, new jobs queue up rather than being
 *   rejected. For a scheduler this is safer than RejectedExecutionException.
 *   In production you'd cap the queue size and handle rejection explicitly.
 */
public class ThreadPoolTaskExecutor implements ITaskExecutor {

    private final ThreadPoolExecutor pool;

    public ThreadPoolTaskExecutor(int threads) {
        this.pool = new ThreadPoolExecutor(
            threads, threads,       // core == max: fixed pool, no thread recycling delay
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, "job-worker");
                // Daemon threads: JVM exits even if worker threads are running.
                // Prevents the process from hanging after shutdown() is called.
                t.setDaemon(true);
                return t;
            }
        );
    }

    @Override
    public void execute(Runnable task) {
        // pool.submit wraps the Runnable in a Future, which silently swallows
        // exceptions. runJob() in JobScheduler has its own try-catch, so this
        // is intentional — exception handling is our responsibility, not the pool's.
        pool.submit(task);
    }

    @Override
    public void shutdown() {
        // Orderly shutdown: queued tasks are not cancelled, but no new ones accepted.
        pool.shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
