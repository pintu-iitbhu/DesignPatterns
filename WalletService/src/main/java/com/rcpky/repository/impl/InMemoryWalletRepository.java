package com.rcpky.repository.impl;

import com.rcpky.model.Wallet;
import com.rcpky.repository.IWalletRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryWalletRepository implements IWalletRepository {
    private final Map<String, Wallet> store = new ConcurrentHashMap<>();

    @Override
    public Wallet save(Wallet wallet) {
        store.put(wallet.getId(), wallet);
        return wallet;
    }

    @Override
    public Optional<Wallet> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Wallet> findAll() {
        return new ArrayList<>(store.values());
    }
}
