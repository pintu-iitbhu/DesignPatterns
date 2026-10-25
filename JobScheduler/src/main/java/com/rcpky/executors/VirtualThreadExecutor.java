package com.rcpky.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadExecutor implements ITaskExecutor{

    private final int threads;
    private final ExecutorService pool;


    public  VirtualThreadExecutor(int threads) {
        this.threads = threads;
        this.pool= Executors.newVirtualThreadPerTaskExecutor();

    }

    @Override
    public void execute(Runnable task) {
        pool.submit(task);
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
