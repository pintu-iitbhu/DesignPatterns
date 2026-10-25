package com.rcpky;

import com.rcpky.scheduler.JobScheduler;

import java.util.concurrent.atomic.AtomicInteger;

public class App {

    public static void main(String[] args) throws InterruptedException {
        JobScheduler scheduler = new JobScheduler(4);

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

        System.out.println("\n=== Shutdown ===");
        scheduler.shutdown();
        System.out.println("Done.");
    }
}
