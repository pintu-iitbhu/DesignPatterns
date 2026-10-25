package com.rcpky.models;

/**
 * Enum with behaviour — each constant carries its own ReschedulingPolicy
 * (Strategy Pattern). This means adding a new job type (e.g. CRON) only
 * requires adding an enum value; JobScheduler.reschedule() is untouched.
 *
 * Why put the policy on the enum instead of a plain if/else in JobScheduler?
 *   The if/else violates the Open/Closed Principle: every new type forces a
 *   change inside the scheduler. With this approach, the enum IS the switch.
 */
public enum JobType {

    // Runs exactly once at the scheduled time, then removed.
    ONCE(null),

    // Recurring: next run is computed from the last *scheduled* time.
    // Maintains a consistent wall-clock cadence even if execution is slow.
    FIXED_RATE(ReschedulingPolicy.FIXED_RATE),

    // Recurring: next run is computed from the moment execution *finishes*.
    // Guarantees a fixed gap between the end of one run and the start of the next.
    FIXED_DELAY(ReschedulingPolicy.FIXED_DELAY);

    private final ReschedulingPolicy policy;

    JobType(ReschedulingPolicy policy) {
        this.policy = policy;
    }

    // Used in runJob() to decide whether to reschedule or clean up after execution.
    public boolean isRecurring() {
        return policy != null;
    }

    // Delegates next-run-time calculation to the policy lambda.
    // JobScheduler.reschedule() calls this — no type-specific logic leaks out.
    public long nextRunTime(long lastScheduled, long period) {
        if (policy == null) throw new IllegalStateException("ONCE jobs cannot be rescheduled");
        return policy.nextRunTime(lastScheduled, period);
    }
}
