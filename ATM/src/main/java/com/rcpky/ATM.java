package com.rcpky;

import com.rcpky.enums.ATMState;
import com.rcpky.enums.CardStatus;
import com.rcpky.enums.TransactionType;
import com.rcpky.exceptions.CardBlockedException;
import com.rcpky.exceptions.DuplicateTransactionException;
import com.rcpky.exceptions.InsufficientBalanceException;
import com.rcpky.exceptions.InsufficientCashException;
import com.rcpky.exceptions.InvalidPinException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core ATM class managing the user session state machine:
 *   IDLE -> CARD_INSERTED -> AUTHENTICATED -> DISPENSING_CASH -> AUTHENTICATED -> IDLE
 *
 * Thread safety:
 *  - sessionLock ensures only one user session runs at a time.
 *  - BankAccount uses its own ReentrantLock for balance operations.
 *  - CashDispenser uses its own ReentrantLock for cash operations.
 *
 * Rollback guarantee:
 *  - Account is debited BEFORE cash is dispensed.
 *  - If dispensing fails, the debit is credited back and the transaction is marked ROLLED_BACK.
 *
 * Duplicate prevention:
 *  - Each withdrawal accepts an idempotency key (requestId).
 *  - If the same requestId is submitted again, DuplicateTransactionException is thrown immediately.
 */
public class ATM {

    private final String atmId;
    private final CashDispenser cashDispenser;
    private final Bank bank;

    private ATMState state;
    private Card currentCard;
    private BankAccount currentAccount;

    private final ReentrantLock sessionLock;

    public ATM(String atmId, CashDispenser cashDispenser, Bank bank) {
        this.atmId = atmId;
        this.cashDispenser = cashDispenser;
        this.bank = bank;
        this.state = ATMState.IDLE;
        this.sessionLock = new ReentrantLock();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void insertCard(String cardNumber) {
        sessionLock.lock();
        try {
            if (state != ATMState.IDLE) {
                throw new IllegalStateException("[" + atmId + "] ATM is currently in use. Please wait.");
            }
            Card card = bank.getCard(cardNumber);
            if (card == null) {
                throw new IllegalArgumentException("[" + atmId + "] Card not found: " + cardNumber);
            }
            if (card.getCardStatus() == CardStatus.BLOCKED) {
                throw new CardBlockedException("[" + atmId + "] Card is blocked: " + cardNumber);
            }
            if (card.isExpired()) {
                throw new CardBlockedException("[" + atmId + "] Card is expired: " + cardNumber);
            }
            currentCard = card;
            state = ATMState.CARD_INSERTED;
            System.out.println("[" + atmId + "] Card inserted: " + cardNumber);
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Validates the PIN. On 3rd consecutive failure the account is locked
     * and the card is blocked and ejected automatically.
     */
    public void authenticatePin(String pin) {
        sessionLock.lock();
        try {
            if (state != ATMState.CARD_INSERTED) {
                throw new IllegalStateException("[" + atmId + "] Please insert a card first.");
            }
            BankAccount account = bank.getAccount(currentCard.getLinkedAccountId());
            if (account == null) {
                throw new IllegalStateException("[" + atmId + "] No account linked to this card.");
            }
            if (account.isLocked()) {
                currentCard.block();
                ejectCardInternal();
                throw new CardBlockedException("[" + atmId + "] Account already locked. Card has been blocked and ejected.");
            }
            if (!account.validatePin(pin)) {
                if (account.isLocked()) {
                    currentCard.block();
                    ejectCardInternal();
                    throw new CardBlockedException("[" + atmId + "] Too many failed PIN attempts. Card blocked and ejected.");
                }
                throw new InvalidPinException("[" + atmId + "] Invalid PIN. Please try again.");
            }
            currentAccount = account;
            state = ATMState.AUTHENTICATED;
            System.out.println("[" + atmId + "] Authentication successful. Welcome, " + account.getAccountHolder() + "!");
        } finally {
            sessionLock.unlock();
        }
    }

    public double checkBalance() {
        sessionLock.lock();
        try {
            ensureAuthenticated();
            Transaction txn = new Transaction(
                    UUID.randomUUID().toString(),
                    currentAccount.getAccountId(),
                    TransactionType.BALANCE_ENQUIRY,
                    0);
            bank.recordTransaction(txn);
            double balance = currentAccount.getBalance();
            txn.markSuccess();
            System.out.println("[" + atmId + "] Balance enquiry -> " + txn);
            System.out.println("[" + atmId + "] Available balance: Rs." + balance);
            return balance;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Withdraws cash.
     *
     * @param amount     amount to withdraw (must be dispensable with available denominations)
     * @param requestId  idempotency key supplied by the client; pass null to auto-generate.
     *                   Submitting the same non-null requestId twice throws DuplicateTransactionException.
     */
    public Transaction withdrawCash(int amount, String requestId) {
        sessionLock.lock();
        try {
            ensureAuthenticated();

            String txnId = (requestId != null) ? requestId : UUID.randomUUID().toString();

            if (bank.isTransactionProcessed(txnId)) {
                throw new DuplicateTransactionException(
                        "[" + atmId + "] Duplicate transaction rejected. RequestId=" + txnId);
            }

            Transaction txn = new Transaction(txnId, currentAccount.getAccountId(),
                    TransactionType.WITHDRAWAL, amount);
            bank.recordTransaction(txn);

            try {
                if (!cashDispenser.hasSufficientCash(amount)) {
                    txn.markFailed();
                    throw new InsufficientCashException(
                            "[" + atmId + "] ATM has insufficient cash. Available: "
                            + cashDispenser.getTotalCash() + ", Requested: " + amount);
                }

                currentAccount.debit(amount);

                try {
                    state = ATMState.DISPENSING_CASH;
                    Map<Integer, Integer> dispensed = cashDispenser.dispenseCash(amount);
                    txn.markSuccess();
                    state = ATMState.AUTHENTICATED;
                    System.out.println("[" + atmId + "] Cash dispensed: " + dispensed);
                    System.out.println("[" + atmId + "] " + txn);
                    return txn;

                } catch (InsufficientCashException e) {
                    currentAccount.credit(amount);
                    txn.markRolledBack();
                    state = ATMState.AUTHENTICATED;
                    System.out.println("[" + atmId + "] ROLLBACK executed — account re-credited Rs." + amount);
                    System.out.println("[" + atmId + "] " + txn);
                    throw e;
                }

            } catch (InsufficientBalanceException e) {
                txn.markFailed();
                System.out.println("[" + atmId + "] " + txn);
                throw e;
            }

        } finally {
            sessionLock.unlock();
        }
    }

    public void ejectCard() {
        sessionLock.lock();
        try {
            ejectCardInternal();
        } finally {
            sessionLock.unlock();
        }
    }

    public ATMState getState() { return state; }
    public String getAtmId()   { return atmId; }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void ejectCardInternal() {
        if (currentCard != null) {
            System.out.println("[" + atmId + "] Card ejected: " + currentCard.getCardNumber());
        }
        currentCard = null;
        currentAccount = null;
        state = ATMState.IDLE;
    }

    private void ensureAuthenticated() {
        if (state != ATMState.AUTHENTICATED) {
            throw new IllegalStateException(
                    "[" + atmId + "] Action requires authentication. Current state: " + state);
        }
    }
}
