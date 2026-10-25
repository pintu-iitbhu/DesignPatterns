package com.rcpky.models;

/**
 * Strategy Pattern — defines the algorithm for computing when a recurring
 * job should run next. Each JobType carries one of these policies.
 *
 * Why @FunctionalInterface?
 *   Allows each policy to be expressed as a concise lambda (see JobType).
 *   Adding a new scheduling strategy (e.g. EXPONENTIAL_BACKOFF, CRON) only
 *   requires a new lambda constant here and a new enum value in JobType.
 *   JobScheduler.reschedule() needs zero changes — Open/Closed Principle.
 */
@FunctionalInterface
public interface ReschedulingPolicy {

    long nextRunTime(long lastScheduledTime, long period);

    // FIXED_RATE: next run is relative to when the job was *scheduled* to run.
    // This compensates for execution time, keeping a consistent cadence.
    // Example: period=1000ms, execution took 200ms → next fires in 800ms.
    ReschedulingPolicy FIXED_RATE  = (lastScheduled, period) -> lastScheduled + period;

    // FIXED_DELAY: next run is relative to when the job *finished* executing.
    // Guarantees a minimum gap between end of one run and start of the next.
    // Example: period=1000ms, execution took 200ms → next fires in 1000ms.
    ReschedulingPolicy FIXED_DELAY = (lastScheduled, period) -> System.currentTimeMillis() + period;
}
