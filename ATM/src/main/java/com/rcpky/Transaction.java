package com.rcpky;

import com.rcpky.enums.TransactionStatus;
import com.rcpky.enums.TransactionType;

import java.time.LocalDateTime;

public class Transaction {
    private final String transactionId;
    private final String accountId;
    private final TransactionType type;
    private final double amount;
    private TransactionStatus status;
    private final LocalDateTime timestamp;

    public Transaction(String transactionId, String accountId, TransactionType type, double amount) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.status = TransactionStatus.PENDING;
        this.timestamp = LocalDateTime.now();
    }

    public void markSuccess()    { this.status = TransactionStatus.SUCCESS; }
    public void markFailed()     { this.status = TransactionStatus.FAILED; }
    public void markRolledBack() { this.status = TransactionStatus.ROLLED_BACK; }

    public String getTransactionId()    { return transactionId; }
    public String getAccountId()        { return accountId; }
    public TransactionType getType()    { return type; }
    public double getAmount()           { return amount; }
    public TransactionStatus getStatus(){ return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] TxID=%.8s | Account=%s | Type=%s | Amount=%.2f | Status=%s",
                timestamp, transactionId, accountId, type, amount, status);
    }
}
