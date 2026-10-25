package com.rcpky.scheduler;

import com.rcpky.executors.ITaskExecutor;
import com.rcpky.executors.ThreadPoolTaskExecutor;
import com.rcpky.models.Job;
import com.rcpky.models.JobStatus;
import com.rcpky.models.JobType;
import com.rcpky.store.IJobQueue;
import com.rcpky.store.PriorityJobQueue;

import java.util.concurrent.TimeUnit;

/**
 * Orchestrates job dispatch. Single Responsibility: it only decides
 * WHAT to do with a job once the queue says it's due. It does NOT own
 * storage (IJobQueue) or execution (ITaskExecutor) — both are injected.
 *
 * Thread model:
 *   dispatcher thread  — single thread, calls jobQueue.awaitNext() in a loop.
 *                         Blocks inside the queue until a job is due, then
 *                         hands it to the worker pool. Never does real work.
 *   worker thread pool — N threads executing job tasks concurrently.
 *                         Slow jobs don't block the dispatcher or each other.
 */
public class JobScheduler implements IJobScheduler {

    private final IJobQueue jobQueue;
    private final ITaskExecutor taskExecutor;

    // volatile: written by shutdown() on the caller thread,
    // read by the dispatcher thread. No lock needed for a single boolean flag.
    private volatile boolean running;
    private final Thread dispatcher;

    // Convenience constructor — wires up default implementations.
    public JobScheduler(int workerThreads) {
        this(new PriorityJobQueue(), new ThreadPoolTaskExecutor(workerThreads));
    }

    // Full DI constructor — used in tests to inject mocks/stubs.
    // Starts the dispatcher thread immediately after wiring dependencies.
    public JobScheduler(IJobQueue jobQueue, ITaskExecutor taskExecutor) {
        this.jobQueue = jobQueue;
        this.taskExecutor = taskExecutor;
        this.running = true;
        this.dispatcher = new Thread(this::dispatch, "job-dispatcher");
        this.dispatcher.setDaemon(true); // doesn't prevent JVM exit
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
                if (job == null) break;         // queue shut down, exit loop

                // Lazy deletion check: job was polled from the heap but may have
                // been cancelled while it was waiting. Skip without executing.
                if (job.getStatus() != JobStatus.CANCELLED) {
                    taskExecutor.execute(() -> runJob(job));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Runs on a worker thread. Three possible outcomes after execution:
     *   1. ONCE job succeeds  → mark COMPLETED, remove from registry.
     *   2. Recurring job succeeds → reschedule (re-enqueue with new nextRunTime).
     *   3. Task throws        → mark FAILED, do not reschedule.
     *
     * CAS PENDING→RUNNING at the start prevents double-execution if two
     * threads somehow both get the same job (defensive, shouldn't happen
     * with a single dispatcher but guards against future changes).
     */
    private void runJob(Job job) {
        if (!job.compareAndSetStatus(JobStatus.PENDING, JobStatus.RUNNING)) {
            return; // another thread already owns this job; bail out
        }
        try {
            job.getTask().run();
            if (job.getType().isRecurring()) {
                reschedule(job);
            } else {
                job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.COMPLETED);
                jobQueue.remove(job.getId()); // clean up registry for ONCE jobs
            }
        } catch (Exception e) {
            job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.FAILED);
            // Do not rethrow: pool.submit() would silently swallow it anyway.
            // In production, hook an Observer here for alerting/metrics.
            System.err.println("[JobScheduler] Job " + job.getId() + " failed: " + e.getMessage());
        }
    }

    /**
     * Re-enqueues a recurring job for its next execution.
     *
     * Registry check first: if the job was cancelled while it was RUNNING,
     * jobQueue.remove() already deleted it from the registry. contains()
     * returns false here and we silently drop the reschedule — the job
     * effectively stops after the current execution finishes.
     *
     * nextRunTime is delegated to JobType (Strategy Pattern) so this method
     * has zero knowledge of FIXED_RATE vs FIXED_DELAY semantics.
     */
    private void reschedule(Job job) {
        if (!jobQueue.contains(job.getId())) return; // was cancelled; stop recurring

        long nextRunTime = job.getType().nextRunTime(job.getNextRunTime(), job.getPeriod());
        job.setNextRunTime(nextRunTime);
        job.compareAndSetStatus(JobStatus.RUNNING, JobStatus.PENDING); // ready for next pick-up
        jobQueue.enqueue(job); // back into the min-heap
    }

    @Override
    public String scheduleOnce(Runnable task, long delayMs) {
        Job job = new Job(task, delayMs, 0, JobType.ONCE);
        jobQueue.enqueue(job); // enqueue handles registry.put + heap.offer + signal
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
     * Cancel by ID. Removes from registry immediately (O(1)).
     * The job may still be in the PriorityQueue (lazy deletion) — when it
     * eventually reaches the head, dispatch() checks CANCELLED status and skips it.
     *
     * CAS PENDING→CANCELLED: if the job is already RUNNING, the CAS fails
     * (status stays RUNNING). The running task completes, but reschedule()
     * finds the job missing from the registry and does not re-enqueue it.
     */
    @Override
    public boolean cancel(String jobId) {
        Job job = jobQueue.remove(jobId); // atomic registry removal
        if (job == null) return false;    // already done/cancelled/unknown
        job.compareAndSetStatus(JobStatus.PENDING, JobStatus.CANCELLED);
        return true;
    }

    /**
     * Graceful shutdown sequence:
     *   1. Set running=false so the dispatch loop exits after the current iteration.
     *   2. jobQueue.shutdown() wakes the dispatcher if it's blocked in awaitNext().
     *   3. taskExecutor.shutdown() stops accepting new tasks.
     *   4. awaitTermination() waits up to 5s for in-flight worker tasks to finish.
     */
    @Override
    public void shutdown() {
        running = false;
        jobQueue.shutdown();    // unblocks dispatcher thread
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
