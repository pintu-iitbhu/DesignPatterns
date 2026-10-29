package com.rcpky.dlq;

import com.rcpky.models.JobType;

/**
 * Immutable snapshot of a job that has exhausted all retry attempts.
 *
 * Stores enough information to diagnose the failure and, if needed,
 * replay the job manually or via a separate DLQ processor.
 *
 * Why a separate class instead of re-using Job?
 *   Job is a live scheduling object with mutable state (retryCount,
 *   nextRunTime, status). A DeadLetterEntry is a historical record —
 *   immutable, cheap to read, safe to pass across threads without locks.
 */
public class DeadLetterEntry {

    private final String jobId;
    private final JobType jobType;
    private final String failureReason;
    private final int totalAttempts;    // 1 = original + 0 retries, 2 = original + 1 retry, ...
    private final long deadLetteredAt;  // epoch ms when the entry was created

    public DeadLetterEntry(String jobId, JobType jobType, String failureReason,
                           int totalAttempts, long deadLetteredAt) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.failureReason = failureReason;
        this.totalAttempts = totalAttempts;
        this.deadLetteredAt = deadLetteredAt;
    }

    public String getJobId()          { return jobId; }
    public JobType getJobType()       { return jobType; }
    public String getFailureReason()  { return failureReason; }
    public int getTotalAttempts()     { return totalAttempts; }
    public long getDeadLetteredAt()   { return deadLetteredAt; }

    @Override
    public String toString() {
        return "DeadLetterEntry{jobId='" + jobId + "', type=" + jobType
                + ", reason='" + failureReason + "', attempts=" + totalAttempts
                + ", at=" + deadLetteredAt + '}';
    }
}
