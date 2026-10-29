package com.rcpky;

import com.rcpky.dlq.InMemoryDeadLetterQueue;
import com.rcpky.scheduler.JobScheduler;

import java.util.concurrent.atomic.AtomicInteger;

public class App {

    public static void main(String[] args) throws InterruptedException {

        // ------------------------------------------------------------------ Scenarios 1-4 (original)
        InMemoryDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        JobScheduler scheduler = new JobScheduler(4, dlq);

        System.out.println("=== Scenario 1: One-time job with 500ms delay ===");
        scheduler.scheduleOnce(
            () -> System.out.println("[ONCE] Fired at: " + System.currentTimeMillis()),
            500
        );

        System.out.println("\n=== Scenario 2: Fixed-rate job every 300ms ===");
        AtomicInteger rateCount = new AtomicInteger(0);
        String fixedRateId = scheduler.scheduleAtFixedRate(
            () -> System.out.println("[FIXED_RATE] Run #" + rateCount.incrementAndGet()),
            0, 300
        );

        System.out.println("\n=== Scenario 3: Fixed-delay job (300ms delay after each run, 200ms execution) ===");
        AtomicInteger delayCount = new AtomicInteger(0);
        scheduler.scheduleWithFixedDelay(
            () -> {
                System.out.println("[FIXED_DELAY] Run #" + delayCount.incrementAndGet());
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            },
            0, 300
        );

        Thread.sleep(1500);

        System.out.println("\n=== Scenario 4: Cancel fixed-rate job ===");
        boolean cancelled = scheduler.cancel(fixedRateId);
        System.out.println("[CANCEL] Cancelled fixed-rate job: " + cancelled);
        System.out.println("[CANCEL] Cancel non-existent job: " + scheduler.cancel("bad-id"));

        Thread.sleep(600);

        // ------------------------------------------------------------------ Scenario 5: Retries with exponential backoff
        System.out.println("\n=== Scenario 5: Retries with exponential backoff ===");
        AtomicInteger retryAttempts = new AtomicInteger(0);
        scheduler.scheduleWithOptions(
            () -> {
                int attempt = retryAttempts.incrementAndGet();
                System.out.println("[RETRY] Attempt #" + attempt);
                if (attempt < 3) {
                    throw new RuntimeException("transient error on attempt " + attempt);
                }
                System.out.println("[RETRY] Succeeded on attempt #" + attempt);
            },
            0,          // no initial delay
            3,          // maxRetries: up to 3 additional attempts (4 total)
            100L,       // base retry delay: 100ms → 100ms, 200ms, 400ms
            0L          // no lease timeout
        );
        Thread.sleep(1500); // wait for retries to complete

        // ------------------------------------------------------------------ Scenario 6: Lease expiry → DLQ
        System.out.println("\n=== Scenario 6: Lease expiry → dead-letter queue ===");
        scheduler.scheduleWithOptions(
            () -> {
                System.out.println("[LEASE] Task started, will exceed lease...");
                try { Thread.sleep(5000); } catch (InterruptedException e) {
                    System.out.println("[LEASE] Task interrupted by lease expiry");
                    Thread.currentThread().interrupt();
                }
            },
            0,      // no initial delay
            0,      // maxRetries=0: no retries, goes straight to DLQ
            1000L,  // retryDelay (unused since maxRetries=0)
            300L    // lease: 300ms timeout
        );
        Thread.sleep(800); // wait for lease to expire + DLQ write

        // ------------------------------------------------------------------ Scenario 7: Lease + retry → eventual success
        System.out.println("\n=== Scenario 7: Lease expiry → retry → success ===");
        AtomicInteger leaseRetryCount = new AtomicInteger(0);
        scheduler.scheduleWithOptions(
            () -> {
                int n = leaseRetryCount.incrementAndGet();
                System.out.println("[LEASE+RETRY] Attempt #" + n);
                if (n == 1) {
                    // First attempt: hang past the lease
                    try { Thread.sleep(5000); } catch (InterruptedException e) {
                        System.out.println("[LEASE+RETRY] Lease expired, will retry");
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.println("[LEASE+RETRY] Succeeded on attempt #" + n);
                }
            },
            0,      // no initial delay
            1,      // maxRetries=1: one retry allowed
            150L,   // retry base delay: 150ms
            250L    // lease: 250ms timeout
        );
        Thread.sleep(1500);

        System.out.println("\n=== DLQ summary: " + dlq.size() + " dead-lettered job(s) ===");
        dlq.getAll().forEach(e -> System.out.println("  " + e));

        System.out.println("\n=== Shutdown ===");
        scheduler.shutdown();
        System.out.println("Done.");
    }
}
