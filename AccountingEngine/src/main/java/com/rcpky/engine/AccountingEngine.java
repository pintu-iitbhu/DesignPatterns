package com.rcpky.engine;

import com.rcpky.audit.AuditEvent;
import com.rcpky.domain.Transaction;
import com.rcpky.ledger.LedgerEntry;
import com.rcpky.rule.Rule;

import java.util.List;

public interface AccountingEngine {
    EngineResult evaluate(Transaction transaction);
    void addRule(Rule rule);
    void removeRule(String ruleId);
    void updateRule(Rule rule);
    List<AuditEvent> getAuditTrail(String transactionId);
    List<LedgerEntry> getLedgerEntries(String transactionId);
}
