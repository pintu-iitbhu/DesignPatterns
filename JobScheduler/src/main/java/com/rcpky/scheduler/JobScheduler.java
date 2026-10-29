package com.rcpky.scheduler;

import com.rcpky.dlq.DeadLetterEntry;
import com.rcpky.dlq.IDeadLetterQueue;
import com.rcpky.executors.CachedThreadPoolTaskExecutor;
import com.rcpky.executors.ITaskExecutor;
import com.rcpky.executors.ThreadPoolTaskExecutor;
import com.rcpky.models.Job;
import com.rcpky.models.JobStatus;
import com.rcpky.models.JobType;
import com.rcpky.store.IJobQueue;
import com.rcpky.store.PriorityJobQueue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates job dispatch. Single Responsibility: it only decides
 * WHAT to do with a job once the queue says it's due. Storage (IJobQueue),
 * execution (ITaskExecutor), and dead-lettering (IDeadLetterQueue) are
 * all injected dependencies.
 *
 * Thread model:
 *   dispatcher thread  — single thread; calls jobQueue.awaitNext() in a loop.
 *                         Hands due jobs to either taskExecutor or leaseExecutor.
 *   taskExecutor       — fixed pool; runs NON-leased jobs only. Never blocks.
 *   leaseExecutor      — unbounded cached pool; runs leased jobs.
 *                         Two threads are consumed per leased job:
 *                           Thread B (monitor) — runs runJob(), blocks on Future.get(leaseMs)
 *                           Thread C (worker)  — runs job.getTask() via leaseExecutor.submit()
 *                         Because the cached pool is unbounded, blocking Thread B never
 *                         starves Thread C or any other job. Fixed pool is never touched.
 */
public class JobScheduler implements IJobScheduler {

    private final IJobQueue jobQueue;
    private final ITaskExecutor taskExecutor;

    // Nullable: null means no DLQ — failed jobs just transition to FAILED.
    private final IDeadLetterQueue dlq;

    // Dedicated executor for tasks that run under a lease timeout.
    // Injected as ITaskExecutor — defaults to CachedThreadPoolTaskExecutor so
    // it never deadlocks with the fixed-size taskExecutor pool. Callers can
    // substitute any ITaskExecutor impl (e.g. VirtualThreadExecutor for I/O-heavy jobs).
    private final ITaskExecutor leaseExecutor;

    // volatile: written by shutdown() on the caller thread,
    // read by the dispatcher thread. No lock needed for a single boolean flag.
    private volatile boolean running;
    private final Thread dispatcher;

    // Convenience constructor — no DLQ, no retries, no leases (legacy behaviour).
    public JobScheduler(int workerThreads) {
        this(new PriorityJobQueue(), new ThreadPoolTaskExecutor(workerThreads),
                new CachedThreadPoolTaskExecutor("lease-worker"), null);
    }

    // Constructor with DLQ — enables dead-lettering of exhausted jobs.
    public JobScheduler(int workerThreads, IDeadLetterQueue dlq) {
        this(new PriorityJobQueue(), new ThreadPoolTaskExecutor(workerThreads),
                new CachedThreadPoolTaskExecutor("lease-worker"), dlq);
    }

    // Full DI constructor — used in tests to inject mocks/stubs.
    // leaseExecutor is a separate ITaskExecutor so callers can swap the execution
    // strategy (e.g. VirtualThreadExecutor for I/O-heavy leased jobs).
    public JobScheduler(IJobQueue jobQueue, ITaskExecutor taskExecutor,
                        ITaskExecutor leaseExecutor, IDeadLetterQueue dlq) {
        this.jobQueue = jobQueue;
        this.taskExecutor = taskExecutor;
        this.leaseExecutor = leaseExecutor;
        this.dlq = dlq;
        this.running = true;
        this.dispatcher = new Thread(this::dispatch, "job-dispatcher");
        this.dispatcher.setDaemon(true);
        this.dispatcher.start();
    }

    /**
     * Dispatcher loop. Kept intentionally simple — all complexity is in
     * IJobQueue.awaitNext(). This method only:
     *   1. Asks the queue for the next due job (blocks until one is ready).
     *   2. Skips cancelled jobs (lazy deletion: they stay in the heap until polled).
     *   3. Submits due, non-cancelled jobs to the worker pool.
     */
    private void dispatch() {
        while (running) {
            try {
                Job job = jobQueue.awaitNext(); // blocks; returns null on shutdown
                if (job == null) break;

                // Lazy deletion check: job was cancelled while waiting in the heap.
                if (job.getStatus() != JobStatus.CANCELLED) {
                    // Leased jobs go to the unbounded cached pool so that the
                    // monitor thread (blocking on future.get) never occupies a
                    // fixed-pool slot. Non-leased jobs run on the fixed pool.
                    if (job.getLeaseTimeoutMs() > 0) {
                        leaseExecutor.execute(() -> runJob(job));
                    } else {
                        taskExecutor.execute(() -> runJob(job));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Runs on a worker thread. Outcomes:
     *   • Lease timeout  → handleFailure (retry or DLQ)
     *   • Task exception → handleFailure (retry or DLQ)
     *   • Success + ONCE → COMPLETED, remove from registry
     *   • Success + recurring → reschedule
     *
     * Lease enforcement: when leaseTimeoutMs > 0 the actual task is submitted
     * to a separate leaseExecutor and this thread blocks on Future.get(timeout).
     * On TimeoutException the Future is cancelled (interrupt sent) and the job
     * is treated as failed. Using a separate executor avoids deadlocks with the
     * fixed-size worker pool.
     *
     * CAS PENDING→RUNNING prevents double-execution (defensive guard).
     */
    private void runJob(Job job) {
        if (!job.compareAndSetStatus(JobStatus.PENDING, JobStatus.RUNNING)) {
            return; // another thread already owns this execution slot
        }

        String failureReason = null;

        try {
            if (job.getLeaseTimeoutMs() > 0) {
                // --- Lease-bounded execution: delegate to ITaskExecutor ---
                Future<?> future = leaseExecutor.submit(job.getTask());
                try {
                    future.get(job.getLeaseTimeoutMs(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true); // interrupt the leased thread
                    failureReason = "Lease expired after " + job.getLeaseTimeoutMs() + " ms";
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    failureReason = cause != null ? cause.getMessage() : "Task execution failed";
                }
            } else {
                // --- Direct execution (no lease) ---
                job.getTask().run();
            }
        } catch (Exception e) {
            failureReason = e.getMessage();
        }

        if (failureReason != null) {
            handleFailure(job, failureReason);
            return;
        }

        // --- Success path ---
        if (job.getType().isRecurring()) {
            reschedule(job);
        } else {
            job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.COMPLETED);
            jobQueue.remove(job.getId());
        }
    }

    /**
     * Called on every job failure (exception or lease expiry).
     *
     * Retry logic:
     *   retriesUsed = retryCount before this failure (0-based).
     *   If retriesUsed < maxRetries → re-enqueue with exponential backoff.
     *   Otherwise → dead-letter (if DLQ is wired) or FAILED (legacy path).
     *
     * Exponential backoff: delay = retryDelayMs * 2^retriesUsed
     *   attempt 0 (first retry)  → retryDelayMs * 1
     *   attempt 1 (second retry) → retryDelayMs * 2
     *   attempt 2 (third retry)  → retryDelayMs * 4
     *   ...
     */
    private void handleFailure(Job job, String reason) {
        int retriesUsed = job.getRetryCount().getAndIncrement(); // read then increment

        if (retriesUsed < job.getMaxRetries()) {
            // Retry: exponential backoff, re-enqueue (stays in registry)
            long backoffMs = job.getRetryDelayMs() * (1L << retriesUsed);
            job.setNextRunTime(System.currentTimeMillis() + backoffMs);
            job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.PENDING);
            jobQueue.enqueue(job);
            System.err.println("[JobScheduler] Job " + job.getId()
                    + " retrying (attempt " + (retriesUsed + 1) + "/" + job.getMaxRetries()
                    + ") in " + backoffMs + " ms — cause: " + reason);
        } else {
            // No retries left → dead-letter or plain FAILED
            jobQueue.remove(job.getId());
            if (dlq != null) {
                job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.DEAD_LETTERED);
                dlq.add(new DeadLetterEntry(
                        job.getId(), job.getType(), reason,
                        retriesUsed + 1, System.currentTimeMillis()));
            } else {
                job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.FAILED);
                System.err.println("[JobScheduler] Job " + job.getId()
                        + " failed after " + (retriesUsed + 1) + " attempt(s): " + reason);
            }
        }
    }

    /**
     * Re-enqueues a recurring job for its next execution.
     *
     * Registry check first: if the job was cancelled while RUNNING,
     * jobQueue.remove() already deleted it. contains() returns false and
     * we silently stop the recurring chain.
     *
     * nextRunTime is delegated to JobType (Strategy Pattern).
     */
    private void reschedule(Job job) {
        if (!jobQueue.contains(job.getId())) return; // cancelled; stop recurring

        long nextRunTime = job.getType().nextRunTime(job.getNextRunTime(), job.getPeriod());
        job.setNextRunTime(nextRunTime);
        job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.PENDING);
        jobQueue.enqueue(job);
    }

    @Override
    public String scheduleOnce(Runnable task, long delayMs) {
        Job job = new Job(task, delayMs, 0, JobType.ONCE);
        jobQueue.enqueue(job);
        return job.getId();
    }

    @Override
    public String scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs) {
        Job job = new Job(task, initialDelayMs, periodMs, JobType.FIXED_RATE);
        jobQueue.enqueue(job);
        return job.getId();
    }

    @Override
    public String scheduleWithFixedDelay(Runnable task, long initialDelayMs, long delayMs) {
        Job job = new Job(task, initialDelayMs, delayMs, JobType.FIXED_DELAY);
        jobQueue.enqueue(job);
        return job.getId();
    }

    /**
     * Schedule a one-shot job with retry and/or lease semantics.
     * Any combination of maxRetries > 0 and leaseTimeoutMs > 0 is valid.
     */
    @Override
    public String scheduleWithOptions(Runnable task, long delayMs,
                                      int maxRetries, long retryDelayMs, long leaseTimeoutMs) {
        Job job = new Job(task, delayMs, 0, JobType.ONCE, maxRetries, retryDelayMs, leaseTimeoutMs);
        jobQueue.enqueue(job);
        return job.getId();
    }

    /**
     * Cancel by ID. Removes from registry immediately (O(1)).
     * If the job is already RUNNING, the CAS PENDING→CANCELLED fails (status
     * stays RUNNING). The task finishes, but reschedule()/handleFailure()
     * find the job missing from the registry and do not re-enqueue it.
     */
    @Override
    public boolean cancel(String jobId) {
        Job job = jobQueue.remove(jobId);
        if (job == null) return false;
        job.compareAndSetStatus(JobStatus.PENDING, JobStatus.CANCELLED);
        return true;
    }

    /**
     * Graceful shutdown:
     *   1. running=false → dispatch loop exits after current iteration.
     *   2. jobQueue.shutdown() → unblocks the dispatcher if sleeping.
     *   3. leaseExecutor.shutdown() → stops accepting new leased tasks.
     *   4. taskExecutor.shutdown() + awaitTermination → drain in-flight workers.
     */
    @Override
    public void shutdown() {
        running = false;
        jobQueue.shutdown();
        leaseExecutor.shutdown();
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
