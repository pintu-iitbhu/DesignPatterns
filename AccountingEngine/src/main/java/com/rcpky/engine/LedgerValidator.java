package com.rcpky.engine;

import com.rcpky.exception.LedgerImbalanceException;
import com.rcpky.ledger.LedgerEntry;
import com.rcpky.ledger.LedgerEntryType;

import java.math.BigDecimal;
import java.util.List;

public class LedgerValidator {

    public void validate(List<LedgerEntry> entries) {
        if (entries.isEmpty()) return;
        BigDecimal totalDebits  = sum(entries, LedgerEntryType.DEBIT);
        BigDecimal totalCredits = sum(entries, LedgerEntryType.CREDIT);
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new LedgerImbalanceException(totalDebits, totalCredits);
        }
    }

    private BigDecimal sum(List<LedgerEntry> entries, LedgerEntryType type) {
        return entries.stream()
                .filter(e -> e.type() == type)
                .map(LedgerEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
