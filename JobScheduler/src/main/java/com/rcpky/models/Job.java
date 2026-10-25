package com.rcpky.models;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a unit of scheduled work.
 *
 * Design choices:
 *  - Immutable config fields (id, task, type, period): set once at creation,
 *    never changed. Safe to read from any thread without locks.
 *  - volatile nextRunTime: written by the worker thread (reschedule) and read
 *    by the dispatcher thread. volatile ensures cross-thread visibility without
 *    needing a full lock for this single field.
 *  - AtomicReference<JobStatus>: status transitions happen across threads
 *    (dispatcher, worker pool, cancel caller). AtomicReference.compareAndSet
 *    (CAS) makes these transitions race-free without a synchronized block.
 */
public class Job implements Comparable<Job> {

    // UUID ensures globally unique IDs without coordination between threads.
    private final String id;

    // Command Pattern: the scheduler knows nothing about what the task does.
    private final Runnable task;

    // Immutable scheduling config — determines rescheduling behaviour.
    private final JobType type;
    private final long period;

    // Mutable: updated by reschedule() before re-enqueueing the job.
    // volatile so the dispatcher always sees the latest value written by workers.
    private volatile long nextRunTime;

    // AtomicReference chosen over synchronized because we only need
    // compareAndSet (CAS) semantics — no compound operations requiring a lock.
    private final AtomicReference<JobStatus> status;

    public Job(Runnable task, long initialDelayMs, long periodMs, JobType type) {
        this.id = UUID.randomUUID().toString();
        this.task = task;
        this.type = type;
        this.period = periodMs;
        // Absolute epoch time at which the job should first fire.
        this.nextRunTime = System.currentTimeMillis() + initialDelayMs;
        this.status = new AtomicReference<>(JobStatus.PENDING);
    }

    public String getId() { return id; }
    public Runnable getTask() { return task; }
    public JobType getType() { return type; }
    public long getPeriod() { return period; }
    public long getNextRunTime() { return nextRunTime; }
    public void setNextRunTime(long nextRunTime) { this.nextRunTime = nextRunTime; }
    public JobStatus getStatus() { return status.get(); }

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
