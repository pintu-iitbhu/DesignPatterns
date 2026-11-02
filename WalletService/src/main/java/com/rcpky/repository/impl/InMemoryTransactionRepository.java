package com.rcpky.repository.impl;

import com.rcpky.model.Transaction;
import com.rcpky.repository.ITransactionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryTransactionRepository implements ITransactionRepository {
    private final List<Transaction> store = new CopyOnWriteArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        store.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByWalletId(String walletId) {
        return store.stream()
                .filter(t -> t.getWalletId().equals(walletId))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(store);
    }
}
