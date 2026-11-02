package com.rcpky.service;

import com.rcpky.exception.WalletException;
import com.rcpky.model.Transaction;
import com.rcpky.model.TransactionType;
import com.rcpky.model.Wallet;
import com.rcpky.repository.ITransactionRepository;
import com.rcpky.repository.IWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class WalletService implements IWalletService {

    private final IWalletRepository walletRepository;
    private final ITransactionRepository transactionRepository;

    // Per-wallet locks — keyed by walletId; lazily created via computeIfAbsent
    private final ConcurrentHashMap<String, ReentrantLock> walletLocks = new ConcurrentHashMap<>();

    // Idempotency caches — keyed by caller-supplied Idempotency-Key header value
    private final ConcurrentHashMap<String, Transaction>       txnIdempotencyCache      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Transaction>> transferIdempotencyCache = new ConcurrentHashMap<>();

    public WalletService(IWalletRepository walletRepository, ITransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    private ReentrantLock lockFor(String walletId) {
        return walletLocks.computeIfAbsent(walletId, k -> new ReentrantLock());
    }

    @Override
    @Transactional
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
    @Transactional(readOnly = true)
    public Wallet getWallet(String walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException("Wallet not found: " + walletId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    @Override
    @Transactional
    public Transaction credit(String walletId, BigDecimal amount, String description, String idempotencyKey) {
        if (idempotencyKey != null) {
            Transaction cached = txnIdempotencyCache.get(idempotencyKey);
            if (cached != null) return cached;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Credit amount must be positive");
        }
        Wallet wallet = getWallet(walletId);
        ReentrantLock lock = lockFor(walletId);
        lock.lock();
        try {
            BigDecimal before = wallet.getBalance();
            BigDecimal after = before.add(amount);
            wallet.setBalance(after);
            walletRepository.save(wallet);
            Transaction t = new Transaction(walletId, TransactionType.CREDIT, amount, before, after,
                    description != null ? description : "Credit", null);
            Transaction saved = transactionRepository.save(t);
            if (idempotencyKey != null) txnIdempotencyCache.putIfAbsent(idempotencyKey, saved);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public Transaction debit(String walletId, BigDecimal amount, String description, String idempotencyKey) {
        if (idempotencyKey != null) {
            Transaction cached = txnIdempotencyCache.get(idempotencyKey);
            if (cached != null) return cached;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Debit amount must be positive");
        }
        Wallet wallet = getWallet(walletId);
        ReentrantLock lock = lockFor(walletId);
        lock.lock();
        try {
            BigDecimal before = wallet.getBalance();
            if (before.compareTo(amount) < 0) {
                throw new WalletException("Insufficient funds: balance=" + before + ", requested=" + amount);
            }
            BigDecimal after = before.subtract(amount);
            wallet.setBalance(after);
            walletRepository.save(wallet);
            Transaction t = new Transaction(walletId, TransactionType.DEBIT, amount, before, after,
                    description != null ? description : "Debit", null);
            Transaction saved = transactionRepository.save(t);
            if (idempotencyKey != null) txnIdempotencyCache.putIfAbsent(idempotencyKey, saved);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public List<Transaction> transfer(String fromWalletId, String toWalletId,
                                      BigDecimal amount, String description, String idempotencyKey) {
        if (idempotencyKey != null) {
            List<Transaction> cached = transferIdempotencyCache.get(idempotencyKey);
            if (cached != null) return cached;
        }
        if (fromWalletId.equals(toWalletId)) {
            throw new WalletException("Cannot transfer to the same wallet");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Transfer amount must be positive");
        }
        Wallet from = getWallet(fromWalletId);
        Wallet to   = getWallet(toWalletId);

        // Consistent lock ordering by ID prevents A→B / B→A deadlock
        String firstId  = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        String secondId = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId   : fromWalletId;
        ReentrantLock lock1 = lockFor(firstId);
        ReentrantLock lock2 = lockFor(secondId);

        lock1.lock();
        try {
            lock2.lock();
            try {
                BigDecimal fromBefore = from.getBalance();
                if (fromBefore.compareTo(amount) < 0) {
                    throw new WalletException("Insufficient funds for transfer");
                }
                from.setBalance(fromBefore.subtract(amount));
                walletRepository.save(from);

                BigDecimal toBefore = to.getBalance();
                to.setBalance(toBefore.add(amount));
                walletRepository.save(to);

                String refId = UUID.randomUUID().toString();
                String desc  = description != null ? description : "Transfer";

                Transaction out = new Transaction(fromWalletId, TransactionType.TRANSFER_OUT, amount,
                        fromBefore, from.getBalance(), desc + " → " + to.getOwnerName(), refId);
                Transaction in  = new Transaction(toWalletId, TransactionType.TRANSFER_IN, amount,
                        toBefore, to.getBalance(), desc + " ← " + from.getOwnerName(), refId);

                transactionRepository.save(out);
                transactionRepository.save(in);
                List<Transaction> result = Arrays.asList(out, in);
                if (idempotencyKey != null) transferIdempotencyCache.putIfAbsent(idempotencyKey, result);
                return result;
            } finally {
                lock2.unlock();
            }
        } finally {
            lock1.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String walletId) {
        getWallet(walletId);
        return transactionRepository.findByWalletId(walletId);
    }
}
