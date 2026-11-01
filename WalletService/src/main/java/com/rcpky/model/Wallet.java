package com.rcpky.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Wallet {
    private final String id;
    private final String ownerName;
    private BigDecimal balance;
    private final String currency;
    private final Instant createdAt;
    private Instant updatedAt;
    private final ReentrantLock lock = new ReentrantLock();

    public Wallet(String ownerName, String currency, BigDecimal initialBalance) {
        this.id = UUID.randomUUID().toString();
        this.ownerName = ownerName;
        this.balance = initialBalance;
        this.currency = currency;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public String getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        this.updatedAt = Instant.now();
    }
}
