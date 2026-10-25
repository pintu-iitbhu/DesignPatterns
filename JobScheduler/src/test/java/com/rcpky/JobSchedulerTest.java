package com.rcpky;

import com.rcpky.scheduler.JobScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class JobSchedulerTest {

    private JobScheduler scheduler;

    @Before
    public void setUp() {
        scheduler = new JobScheduler(4);
    }

    @After
    public void tearDown() {
        scheduler.shutdown();
    }

    @Test
    public void testScheduleOnce_executesExactlyOnce() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.scheduleOnce(() -> { count.incrementAndGet(); latch.countDown(); }, 100);
        assertTrue("Job should execute within 2s", latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(400);
        assertEquals("ONCE job must not repeat", 1, count.get());
    }

    @Test
    public void testScheduleOnce_respectsDelay() throws InterruptedException {
        long[] firedAt = new long[1];
        long scheduledAt = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.scheduleOnce(() -> { firedAt[0] = System.currentTimeMillis(); latch.countDown(); }, 300);
        latch.await(2, TimeUnit.SECONDS);
        assertTrue("Job must not fire before its delay", firedAt[0] >= scheduledAt + 300);
    }

    @Test
    public void testScheduleAtFixedRate_runsMultipleTimes() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        scheduler.scheduleAtFixedRate(latch::countDown, 0, 200);
        assertTrue("Fixed-rate job should run 3 times within 2s", latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testScheduleWithFixedDelay_runsMultipleTimes() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        scheduler.scheduleWithFixedDelay(latch::countDown, 0, 200);
        assertTrue("Fixed-delay job should run 3 times within 3s", latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void testFixedDelay_waitsBetweenRuns() throws InterruptedException {
        long[] lastRun = {0};
        long[] gap = {0};
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);
        long delayMs = 300;
        scheduler.scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();
            if (count.getAndIncrement() == 1) {
                gap[0] = now - lastRun[0];
            }
            lastRun[0] = now;
            latch.countDown();
        }, 0, delayMs);
        latch.await(3, TimeUnit.SECONDS);
        assertTrue("Gap between runs should be >= period", gap[0] >= delayMs);
    }

    @Test
    public void testCancel_stopsFutureExecutions() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        String id = scheduler.scheduleAtFixedRate(count::incrementAndGet, 0, 100);
        Thread.sleep(350);
        scheduler.cancel(id);
        int countAtCancel = count.get();
        Thread.sleep(400);
        assertEquals("Job must not run after cancel", countAtCancel, count.get());
    }

    @Test
    public void testCancel_returnsTrueForExistingJob() throws InterruptedException {
        String id = scheduler.scheduleAtFixedRate(() -> {}, 5000, 5000);
        assertTrue(scheduler.cancel(id));
    }

    @Test
    public void testCancel_returnsFalseForUnknownId() {
        assertFalse(scheduler.cancel("no-such-id"));
    }

    @Test
    public void testShutdown_stopsExecution() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        scheduler.scheduleAtFixedRate(count::incrementAndGet, 0, 100);
        Thread.sleep(350);
        scheduler.shutdown();
        int countAtShutdown = count.get();
        Thread.sleep(400);
        assertEquals("No jobs should run after shutdown", countAtShutdown, count.get());
    }

    @Test
    public void testJobFailure_doesNotCrashScheduler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.scheduleOnce(() -> { throw new RuntimeException("deliberate failure"); }, 0);
        scheduler.scheduleOnce(latch::countDown, 200);
        assertTrue("Scheduler should still run after a job failure", latch.await(2, TimeUnit.SECONDS));
    }
}
