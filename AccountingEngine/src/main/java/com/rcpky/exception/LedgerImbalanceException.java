package com.rcpky.exception;

import java.math.BigDecimal;

public class LedgerImbalanceException extends RuntimeException {
    public LedgerImbalanceException(BigDecimal totalDebits, BigDecimal totalCredits) {
        super(String.format("Ledger imbalance: debits=%s, credits=%s", totalDebits, totalCredits));
    }
}
