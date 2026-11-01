package com.rcpky.service;

import com.rcpky.exception.WalletException;
import com.rcpky.model.Transaction;
import com.rcpky.model.TransactionType;
import com.rcpky.model.Wallet;
import com.rcpky.repository.ITransactionRepository;
import com.rcpky.repository.IWalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService implements IWalletService {

    private final IWalletRepository walletRepository;
    private final ITransactionRepository transactionRepository;

    public WalletService(IWalletRepository walletRepository, ITransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet createWallet(String ownerName, String currency, BigDecimal initialBalance) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new WalletException("Owner name is required");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletException("Initial balance cannot be negative");
        }
        Wallet wallet = new Wallet(ownerName.trim(), currency != null ? currency.toUpperCase() : "USD", initialBalance);
        walletRepository.save(wallet);
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction t = new Transaction(wallet.getId(), TransactionType.CREDIT,
                    initialBalance, BigDecimal.ZERO, initialBalance, "Initial deposit", null);
            transactionRepository.save(t);
        }
        return wallet;
    }

    @Override
    public Wallet getWallet(String walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException("Wallet not found: " + walletId));
    }

    @Override
    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    @Override
    public Transaction credit(String walletId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Credit amount must be positive");
        }
        Wallet wallet = getWallet(walletId);
        wallet.getLock().lock();
        try {
            BigDecimal before = wallet.getBalance();
            BigDecimal after = before.add(amount);
            wallet.setBalance(after);
            Transaction t = new Transaction(walletId, TransactionType.CREDIT, amount, before, after,
                    description != null ? description : "Credit", null);
            return transactionRepository.save(t);
        } finally {
            wallet.getLock().unlock();
        }
    }

    @Override
    public Transaction debit(String walletId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Debit amount must be positive");
        }
        Wallet wallet = getWallet(walletId);
        wallet.getLock().lock();
        try {
            BigDecimal before = wallet.getBalance();
            if (before.compareTo(amount) < 0) {
                throw new WalletException(
                        "Insufficient funds: balance=" + before + ", requested=" + amount);
            }
            BigDecimal after = before.subtract(amount);
            wallet.setBalance(after);
            Transaction t = new Transaction(walletId, TransactionType.DEBIT, amount, before, after,
                    description != null ? description : "Debit", null);
            return transactionRepository.save(t);
        } finally {
            wallet.getLock().unlock();
        }
    }

    @Override
    public List<Transaction> transfer(String fromWalletId, String toWalletId, BigDecimal amount, String description) {
        if (fromWalletId.equals(toWalletId)) {
            throw new WalletException("Cannot transfer to the same wallet");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Transfer amount must be positive");
        }
        Wallet from = getWallet(fromWalletId);
        Wallet to = getWallet(toWalletId);

        // Consistent lock ordering by ID prevents deadlocks
        Wallet first = fromWalletId.compareTo(toWalletId) < 0 ? from : to;
        Wallet second = fromWalletId.compareTo(toWalletId) < 0 ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                BigDecimal fromBefore = from.getBalance();
                if (fromBefore.compareTo(amount) < 0) {
                    throw new WalletException("Insufficient funds for transfer");
                }
                BigDecimal fromAfter = fromBefore.subtract(amount);
                from.setBalance(fromAfter);

                BigDecimal toBefore = to.getBalance();
                BigDecimal toAfter = toBefore.add(amount);
                to.setBalance(toAfter);

                String refId = UUID.randomUUID().toString();
                String desc = description != null ? description : "Transfer";

                Transaction out = new Transaction(fromWalletId, TransactionType.TRANSFER_OUT, amount,
                        fromBefore, fromAfter, desc + " → " + to.getOwnerName(), refId);
                Transaction in = new Transaction(toWalletId, TransactionType.TRANSFER_IN, amount,
                        toBefore, toAfter, desc + " ← " + from.getOwnerName(), refId);

                transactionRepository.save(out);
                transactionRepository.save(in);
                return Arrays.asList(out, in);
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    @Override
    public List<Transaction> getTransactionHistory(String walletId) {
        getWallet(walletId); // validates existence
        return transactionRepository.findByWalletId(walletId);
    }
}
