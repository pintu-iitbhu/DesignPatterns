package com.rcpky.audit;

public enum AuditEventType {
    PROCESSING_STARTED,
    RULE_MATCHED,
    RULE_SKIPPED,
    PROCESSING_COMPLETE,
    DUPLICATE_DETECTED,
    VALIDATION_FAILED
}
