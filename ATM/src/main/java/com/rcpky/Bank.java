package com.rcpky;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton representing the bank backend.
 * Stores accounts, cards, and a ledger of all processed transactions.
 * Uses ConcurrentHashMap for thread-safe access without explicit locking at this layer.
 */
public class Bank {

    private static volatile Bank instance;

    private final Map<String, BankAccount> accounts;
    private final Map<String, Card> cards;
    private final Map<String, Transaction> transactions;
    private final Set<String> processedTransactionIds;

    private Bank() {
        this.accounts = new ConcurrentHashMap<>();
        this.cards = new ConcurrentHashMap<>();
        this.transactions = new ConcurrentHashMap<>();
        this.processedTransactionIds = ConcurrentHashMap.newKeySet();
    }

    public static Bank getInstance() {
        if (instance == null) {
            synchronized (Bank.class) {
                if (instance == null) {
                    instance = new Bank();
                }
            }
        }
        return instance;
    }

    public void addAccount(BankAccount account) {
        accounts.put(account.getAccountId(), account);
    }

    public void addCard(Card card) {
        cards.put(card.getCardNumber(), card);
    }

    public BankAccount getAccount(String accountId) {
        return accounts.get(accountId);
    }

    public Card getCard(String cardNumber) {
        return cards.get(cardNumber);
    }

    /**
     * Returns true if a transaction with this ID has already been recorded,
     * preventing duplicate/replay processing.
     */
    public boolean isTransactionProcessed(String transactionId) {
        return processedTransactionIds.contains(transactionId);
    }

    /**
     * Atomically registers the transaction ID and stores the transaction record.
     * Must be called BEFORE any side effects (debit / dispense) to guarantee
     * idempotency tracking even if the operation subsequently fails.
     */
    public void recordTransaction(Transaction transaction) {
        processedTransactionIds.add(transaction.getTransactionId());
        transactions.put(transaction.getTransactionId(), transaction);
    }

    public Transaction getTransaction(String transactionId) {
        return transactions.get(transactionId);
    }
}
