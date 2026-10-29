package com.rcpky.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * ITaskExecutor backed by an unbounded cached thread pool.
 *
 * Why unbounded for lease enforcement?
 *   The lease executor runs tasks that are already being monitored for timeout
 *   by a worker thread (via Future.get(leaseMs)). If we used the same fixed
 *   pool for both monitoring and execution, every leased job would occupy TWO
 *   worker threads (one blocking, one running) — easy deadlock with a small pool.
 *
 *   A cached pool creates a thread on demand and reuses idle ones. Since lease
 *   tasks are expected to be short-lived (they get cancelled on timeout), the
 *   number of live threads stays bounded in practice.
 *
 * When to prefer this over ThreadPoolTaskExecutor:
 *   Use as the leaseExecutor in JobScheduler. Do NOT use as the primary
 *   taskExecutor — its unbounded nature provides no backpressure, which can
 *   cause OOM under sustained high load.
 */
public class CachedThreadPoolTaskExecutor implements ITaskExecutor {

    private final ExecutorService pool;

    public CachedThreadPoolTaskExecutor(String threadNamePrefix) {
        this.pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, threadNamePrefix);
            t.setDaemon(true); // doesn't prevent JVM exit
            return t;
        });
    }

    @Override
    public void execute(Runnable task) {
        pool.submit(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
