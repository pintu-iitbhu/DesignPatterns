package com.rcpky;

import com.rcpky.exceptions.InsufficientCashException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages physical cash inside the ATM.
 * Holds a denomination map (note value -> count), sorted highest first for greedy dispensing.
 * All public methods are thread-safe via ReentrantLock.
 */
public class CashDispenser {

    private final Map<Integer, Integer> denominationMap;
    private final ReentrantLock lock;

    public CashDispenser() {
        this.denominationMap = new TreeMap<>(Collections.reverseOrder());
        this.lock = new ReentrantLock();
    }

    public void loadCash(Map<Integer, Integer> denominations) {
        lock.lock();
        try {
            denominations.forEach((denom, count) ->
                    denominationMap.merge(denom, count, Integer::sum));
        } finally {
            lock.unlock();
        }
    }

    public int getTotalCash() {
        lock.lock();
        try {
            return totalCashInternal();
        } finally {
            lock.unlock();
        }
    }

    public boolean hasSufficientCash(int amount) {
        lock.lock();
        try {
            return totalCashInternal() >= amount;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Dispenses the requested amount using a greedy algorithm (largest denomination first).
     * If exact change cannot be made, the denominations are rolled back and an exception is thrown.
     *
     * @return map of {denomination -> notes dispensed}
     * @throws InsufficientCashException if total cash or exact change is unavailable
     */
    public Map<Integer, Integer> dispenseCash(int amount) {
        lock.lock();
        try {
            if (totalCashInternal() < amount) {
                throw new InsufficientCashException(
                        "ATM has insufficient cash. Available: " + totalCashInternal() + ", Requested: " + amount);
            }

            Map<Integer, Integer> dispensed = new HashMap<>();
            int remaining = amount;

            for (Map.Entry<Integer, Integer> entry : denominationMap.entrySet()) {
                if (remaining <= 0) break;
                int denom = entry.getKey();
                int available = entry.getValue();
                int needed = Math.min(remaining / denom, available);
                if (needed > 0) {
                    dispensed.put(denom, needed);
                    denominationMap.put(denom, available - needed);
                    remaining -= needed * denom;
                }
            }

            if (remaining > 0) {
                dispensed.forEach((denom, count) ->
                        denominationMap.merge(denom, count, Integer::sum));
                throw new InsufficientCashException(
                        "Cannot dispense exact amount of " + amount + " with available denominations.");
            }

            return dispensed;
        } finally {
            lock.unlock();
        }
    }

    private int totalCashInternal() {
        return denominationMap.entrySet().stream()
                .mapToInt(e -> e.getKey() * e.getValue())
                .sum();
    }

    @Override
    public String toString() {
        return "CashDispenser{total=" + totalCashInternal() + ", denominations=" + denominationMap + "}";
    }
}
