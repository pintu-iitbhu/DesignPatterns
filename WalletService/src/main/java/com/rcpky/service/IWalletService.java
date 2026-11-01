package com.rcpky.service;

import com.rcpky.model.Transaction;
import com.rcpky.model.Wallet;

import java.math.BigDecimal;
import java.util.List;

public interface IWalletService {
    Wallet createWallet(String ownerName, String currency, BigDecimal initialBalance);

    Wallet getWallet(String walletId);

    List<Wallet> getAllWallets();

    Transaction credit(String walletId, BigDecimal amount, String description);

    Transaction debit(String walletId, BigDecimal amount, String description);

    List<Transaction> transfer(String fromWalletId, String toWalletId, BigDecimal amount, String description);

    List<Transaction> getTransactionHistory(String walletId);
}
