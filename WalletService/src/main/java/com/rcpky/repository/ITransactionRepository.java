package com.rcpky.repository;

import com.rcpky.model.Transaction;

import java.util.List;

public interface ITransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findByWalletId(String walletId);

    List<Transaction> findAll();
}
