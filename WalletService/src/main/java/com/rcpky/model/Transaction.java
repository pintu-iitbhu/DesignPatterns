package com.rcpky.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Transaction {
    private final String id;
    private final String walletId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String description;
    private final String referenceId;
    private final Instant timestamp;

    public Transaction(String walletId, TransactionType type, BigDecimal amount,
                       BigDecimal balanceBefore, BigDecimal balanceAfter,
                       String description, String referenceId) {
        this.id = UUID.randomUUID().toString();
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceId = referenceId;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getWalletId() {
        return walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
