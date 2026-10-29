package com.rcpky.models;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a unit of scheduled work.
 *
 * Design choices:
 *  - Immutable config fields (id, task, type, period, maxRetries,
 *    retryDelayMs, leaseTimeoutMs): set once at creation, safe to read
 *    from any thread without locks.
 *  - volatile nextRunTime: written by the worker thread (reschedule/retry)
 *    and read by the dispatcher thread.
 *  - AtomicReference<JobStatus>: CAS makes status transitions race-free.
 *  - AtomicInteger retryCount: incremented by the worker thread on each
 *    failure; read by handleFailure() to decide retry-or-DLQ.
 */
public class Job implements Comparable<Job> {

    // UUID ensures globally unique IDs without coordination between threads.
    private final String id;

    // Command Pattern: the scheduler knows nothing about what the task does.
    private final Runnable task;

    // Immutable scheduling config — determines rescheduling behaviour.
    private final JobType type;
    private final long period;

    // --- Retry config (immutable) ---
    // 0 = no retries (legacy default); N = up to N additional attempts.
    private final int maxRetries;
    // Base delay for exponential backoff: attempt k waits retryDelayMs * 2^(k-1).
    private final long retryDelayMs;

    // --- Lease config (immutable) ---
    // 0 = no timeout; N = task must finish within N ms or it is cancelled.
    private final long leaseTimeoutMs;

    // Mutable: updated by reschedule() / handleFailure() before re-enqueueing.
    // volatile so the dispatcher always sees the latest value written by workers.
    private volatile long nextRunTime;

    // AtomicReference chosen over synchronized because we only need
    // compareAndSet (CAS) semantics — no compound operations requiring a lock.
    private final AtomicReference<JobStatus> status;

    // Incremented on every failed attempt; compared against maxRetries to
    // decide whether to retry or dead-letter.
    private final AtomicInteger retryCount = new AtomicInteger(0);

    // --- Backward-compatible constructor (no retries, no lease) ---
    public Job(Runnable task, long initialDelayMs, long periodMs, JobType type) {
        this(task, initialDelayMs, periodMs, type, 0, 1000L, 0L);
    }

    // --- Full constructor ---
    public Job(Runnable task, long initialDelayMs, long periodMs, JobType type,
               int maxRetries, long retryDelayMs, long leaseTimeoutMs) {
        this.id = UUID.randomUUID().toString();
        this.task = task;
        this.type = type;
        this.period = periodMs;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.leaseTimeoutMs = leaseTimeoutMs;
        // Absolute epoch time at which the job should first fire.
        this.nextRunTime = System.currentTimeMillis() + initialDelayMs;
        this.status = new AtomicReference<>(JobStatus.PENDING);
    }

    public String getId()              { return id; }
    public Runnable getTask()          { return task; }
    public JobType getType()           { return type; }
    public long getPeriod()            { return period; }
    public int getMaxRetries()         { return maxRetries; }
    public long getRetryDelayMs()      { return retryDelayMs; }
    public long getLeaseTimeoutMs()    { return leaseTimeoutMs; }
    public long getNextRunTime()       { return nextRunTime; }
    public void setNextRunTime(long t) { this.nextRunTime = t; }
    public JobStatus getStatus()       { return status.get(); }
    public AtomicInteger getRetryCount() { return retryCount; }

    /**
     * CAS status transition. Returns true only if the current status matches
     * `expected` AND the update succeeds atomically. This prevents two threads
     * from both believing they own the transition (e.g. double-execution).
     */
    public boolean compareAndSetStatus(JobStatus expected, JobStatus update) {
        return status.compareAndSet(expected, update);
    }

    /**
     * PriorityQueue ordering: jobs with the earliest nextRunTime are at the head.
     * Long.compare avoids the overflow bug of casting (a - b) to int when
     * a and b are large epoch millisecond values.
     */
    @Override
    public int compareTo(Job other) {
        return Long.compare(this.nextRunTime, other.nextRunTime);
    }
}
