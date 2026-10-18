package com.rcpky.audit;

import java.util.List;

public interface AuditRepository {
    void save(AuditEvent event);
    List<AuditEvent> findByTransactionId(String transactionId);
    List<AuditEvent> findAll();
}
