package com.rcpky.engine;

import com.rcpky.domain.RiskLevel;
import com.rcpky.ledger.LedgerEntry;

import java.util.List;
import java.util.Set;

public record EngineResult(
        String transactionId,
        EngineStatus status,
        int totalScore,
        RiskLevel riskLevel,
        Set<String> flags,
        Set<String> categories,
        List<LedgerEntry> ledgerEntries,
        List<RuleResult> ruleResults,
        String rejectionReason
) {
    public boolean approved() {
        return status == EngineStatus.APPROVED;
    }
}
