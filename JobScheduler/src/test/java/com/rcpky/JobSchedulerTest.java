package com.rcpky;

import com.rcpky.dlq.InMemoryDeadLetterQueue;
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

    // ------------------------------------------------------------------ retries

    @Test
    public void testRetry_jobSucceedsOnSecondAttempt() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.scheduleWithOptions(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("transient failure");
            }
            latch.countDown();
        }, 0, 2, 100L, 0L);  // maxRetries=2, retryDelay=100ms, no lease

        assertTrue("Job should succeed on 2nd attempt within 3s", latch.await(3, TimeUnit.SECONDS));
        assertEquals("Should have tried exactly 2 times", 2, attempts.get());
    }

    @Test
    public void testRetry_exhaustedRetriesLandsInDlq() throws InterruptedException {
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        JobScheduler dlqScheduler = new JobScheduler(2, dlq);

        try {
            dlqScheduler.scheduleWithOptions(
                    () -> { throw new RuntimeException("always fails"); },
                    0, 2, 50L, 0L);  // 2 retries (3 total attempts)

            Thread.sleep(1500); // enough time for 3 attempts with 50+100ms backoff
            assertEquals("DLQ must have exactly 1 entry", 1, dlq.size());
            assertEquals("Entry should record 3 total attempts",
                    3, dlq.getAll().get(0).getTotalAttempts());
        } finally {
            dlqScheduler.shutdown();
        }
    }

    @Test
    public void testRetry_schedulerRemainsHealthyAfterDlq() throws InterruptedException {
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        JobScheduler dlqScheduler = new JobScheduler(2, dlq);

        try {
            CountDownLatch goodLatch = new CountDownLatch(1);
            dlqScheduler.scheduleWithOptions(
                    () -> { throw new RuntimeException("bad job"); },
                    0, 1, 50L, 0L);
            dlqScheduler.scheduleOnce(goodLatch::countDown, 400);

            assertTrue("Healthy job must run after a DLQ'd job", goodLatch.await(3, TimeUnit.SECONDS));
        } finally {
            dlqScheduler.shutdown();
        }
    }

    // ------------------------------------------------------------------ leases

    @Test
    public void testLease_taskCompletesWithinTimeout_succeeds() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        // Task sleeps 100ms; lease is 500ms → should succeed
        scheduler.scheduleWithOptions(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            latch.countDown();
        }, 0, 0, 1000L, 500L);

        assertTrue("Task within lease should complete", latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testLease_taskExceedsTimeout_isTerminated() throws InterruptedException {
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        JobScheduler dlqScheduler = new JobScheduler(2, dlq);

        try {
            // Task tries to sleep 5s; lease is only 200ms → must be cancelled
            dlqScheduler.scheduleWithOptions(() -> {
                try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, 0, 0, 1000L, 200L);  // maxRetries=0, leaseTimeout=200ms

            Thread.sleep(800); // wait enough for lease to expire and DLQ to receive entry
            assertEquals("Lease-expired job must appear in DLQ", 1, dlq.size());
            assertTrue("Failure reason should mention lease",
                    dlq.getAll().get(0).getFailureReason().contains("Lease expired"));
        } finally {
            dlqScheduler.shutdown();
        }
    }

    @Test
    public void testLease_withRetries_retriesAfterLeaseExpiry() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        JobScheduler dlqScheduler = new JobScheduler(2, dlq);

        try {
            // First attempt: hangs (lease expires). Second attempt: succeeds fast.
            dlqScheduler.scheduleWithOptions(() -> {
                if (attempts.incrementAndGet() == 1) {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                } else {
                    latch.countDown();
                }
            }, 0, 1, 100L, 200L);  // maxRetries=1, retryDelay=100ms, leaseTimeout=200ms

            assertTrue("Job should succeed on retry after lease expiry", latch.await(3, TimeUnit.SECONDS));
            assertEquals("DLQ must be empty — job eventually succeeded", 0, dlq.size());
        } finally {
            dlqScheduler.shutdown();
        }
    }
}
