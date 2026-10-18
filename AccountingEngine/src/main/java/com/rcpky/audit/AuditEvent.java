package com.rcpky.audit;

import java.time.Instant;

public record AuditEvent(
        String id,
        String transactionId,
        AuditEventType type,
        String message,
        Instant timestamp
) {}
