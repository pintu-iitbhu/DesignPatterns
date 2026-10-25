package com.rcpky.models;

/**
 * Lifecycle state machine for a Job.
 * Valid transitions:
 *
 *   PENDING → RUNNING   (dispatcher picks it up; enforced via CAS in runJob)
 *   RUNNING → COMPLETED (ONCE job finishes successfully)
 *   RUNNING → PENDING   (recurring job rescheduled for next run)
 *   RUNNING → FAILED    (task threw an exception)
 *   PENDING → CANCELLED (cancel() called before execution started)
 *
 * There is no RUNNING → CANCELLED transition by design — we cannot
 * forcibly stop a running thread safely. Cancelling a running job marks
 * it so it will NOT be rescheduled after the current execution finishes.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
