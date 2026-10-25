package com.rcpky.store;

import com.rcpky.models.Job;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory job queue backed by a min-heap (PriorityQueue) and a registry map.
 *
 * Two data structures, two responsibilities:
 *   PriorityQueue — orders jobs by nextRunTime so the earliest-due job is
 *                    always at the head. O(log n) insert, O(log n) poll.
 *   ConcurrentHashMap (registry) — O(1) lookup/removal by job ID for
 *                    cancel() and reschedule() checks.
 *
 * Why a single ReentrantLock for the PriorityQueue?
 *   PriorityQueue is NOT thread-safe. peek() + awaitNanos() + poll() must
 *   be atomic — if we released the lock between peek and poll, another
 *   thread could insert an earlier job and we'd miss it. A single lock
 *   covers the entire peek→wait→poll sequence with no race window.
 *
 * Why ConcurrentHashMap for the registry (no lock)?
 *   registry is only read/written for O(1) put/remove/containsKey, never
 *   iterated. ConcurrentHashMap provides segment-level thread safety for
 *   these operations without a global lock, keeping cancel() fast.
 */
public class PriorityJobQueue implements IJobQueue {

    private final PriorityQueue<Job> queue = new PriorityQueue<>();
    private final ConcurrentHashMap<String, Job> registry = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition jobAvailable = lock.newCondition();

    // volatile: written by shutdown() (caller thread), read by awaitNext() (dispatcher).
    private volatile boolean active = true;

    @Override
    public void enqueue(Job job) {
        lock.lock();
        try {
            registry.put(job.getId(), job);  // register for O(1) cancel lookup
            queue.offer(job);                // insert into min-heap
            jobAvailable.signalAll();        // wake dispatcher if it was waiting on empty queue
                                             // or sleeping for a later job (new job may be earlier)
        } finally {
            lock.unlock();
        }
    }

    /**
     * Core blocking method. The dispatcher calls this in a loop.
     *
     * How it works:
     *   1. If the queue is empty, release the lock and park (await) until
     *      enqueue() signals that a new job has arrived.
     *   2. Once the queue has jobs, peek at the head (earliest nextRunTime).
     *   3. If the head is not yet due, release the lock and park for exactly
     *      `delay` nanos. awaitNanos() wakes up either when the timer fires
     *      OR when a new job is enqueued (signalAll in enqueue). This handles
     *      the case where a new earlier-due job arrives during the sleep.
     *   4. On wake, loop back to step 1 and re-evaluate the head.
     *      This re-check is necessary: the head may have changed.
     *   5. When delay <= 0, poll() and return the job to the dispatcher.
     */
    @Override
    public Job awaitNext() throws InterruptedException {
        lock.lock();
        try {
            while (active) {
                // Phase 1: wait if queue is empty
                while (queue.isEmpty() && active) {
                    jobAvailable.await();
                }
                if (!active) return null; // shutdown during empty wait

                // Phase 2: check if the head job is due yet
                long delay = queue.peek().getNextRunTime() - System.currentTimeMillis();
                if (delay <= 0) {
                    // Job is due: remove from heap and hand to dispatcher.
                    // NOTE: we do NOT remove from registry here — that happens
                    // only on completion or cancel (lazy deletion pattern).
                    return queue.poll();
                }

                // Phase 3: job exists but isn't due yet — sleep for exactly
                // `delay` nanos. awaitNanos releases the lock while sleeping,
                // so enqueue() can insert new jobs and wake us early if needed.
                jobAvailable.awaitNanos(TimeUnit.MILLISECONDS.toNanos(delay));
                // Loop back: re-peek in case a new earlier job was enqueued.
            }
            return null; // shutdown
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lazy deletion: removes the job from the registry only, NOT from the
     * PriorityQueue. Removing from PriorityQueue is O(n) which would block
     * the dispatcher. Instead, when the cancelled job reaches the head and
     * gets polled by awaitNext(), the dispatcher checks job.getStatus()
     * and discards it without executing.
     */
    @Override
    public Job remove(String jobId) {
        return registry.remove(jobId);
    }

    // ConcurrentHashMap.containsKey is O(1) and lock-free.
    @Override
    public boolean contains(String jobId) {
        return registry.containsKey(jobId);
    }

    @Override
    public void shutdown() {
        active = false;          // dispatcher's while(active) loop exits
        lock.lock();
        try {
            jobAvailable.signalAll(); // wake dispatcher if blocked in await/awaitNanos
        } finally {
            lock.unlock();
        }
    }
}
