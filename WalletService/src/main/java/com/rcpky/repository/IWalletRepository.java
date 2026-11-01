package com.rcpky.repository;

import com.rcpky.model.Wallet;

import java.util.List;
import java.util.Optional;

public interface IWalletRepository {
    Wallet save(Wallet wallet);

    Optional<Wallet> findById(String id);

    List<Wallet> findAll();
}
