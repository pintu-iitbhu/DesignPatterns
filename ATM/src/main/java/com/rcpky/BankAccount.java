package com.rcpky;

import com.rcpky.exceptions.InsufficientBalanceException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.ReentrantLock;

public class BankAccount {
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final String accountId;
    private final String accountHolder;
    private double balance;
    private final String pinHash;
    private int failedAttempts;
    private boolean locked;
    private final ReentrantLock lock;

    public BankAccount(String accountId, String accountHolder, double balance, String pin) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.pinHash = hashPin(pin);
        this.failedAttempts = 0;
        this.locked = false;
        this.lock = new ReentrantLock();
    }

    public boolean validatePin(String pin) {
        lock.lock();
        try {
            if (locked) return false;

            if (hashPin(pin).equals(pinHash)) {
                failedAttempts = 0;
                return true;
            }

            failedAttempts++;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                locked = true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void debit(double amount) {
        lock.lock();
        try {
            if (balance < amount) {
                throw new InsufficientBalanceException(
                        "Insufficient balance. Available: " + balance + ", Requested: " + amount);
            }
            balance -= amount;
        } finally {
            lock.unlock();
        }
    }

    public void credit(double amount) {
        lock.lock();
        try {
            balance += amount;
        } finally {
            lock.unlock();
        }
    }

    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public boolean isLocked()         { return locked; }
    public String getAccountId()      { return accountId; }
    public String getAccountHolder()  { return accountHolder; }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public String toString() {
        return "BankAccount{id=" + accountId + ", holder=" + accountHolder + ", balance=" + balance + "}";
    }
}
