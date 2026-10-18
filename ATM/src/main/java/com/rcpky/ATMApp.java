package com.rcpky;

import com.rcpky.exceptions.CardBlockedException;
import com.rcpky.exceptions.DuplicateTransactionException;
import com.rcpky.exceptions.InsufficientBalanceException;
import com.rcpky.exceptions.InsufficientCashException;
import com.rcpky.exceptions.InvalidPinException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstrates all ATM flows end-to-end:
 *
 *  1.  Successful balance enquiry
 *  2.  Successful withdrawal
 *  3.  Invalid PIN × 3 → card blocked automatically
 *  4.  Attempting to use a blocked card
 *  5.  Withdrawal with insufficient account balance
 *  6.  Withdrawal when ATM has insufficient cash
 *  7.  Transaction ROLLBACK — account debited but cash dispenser cannot make exact change
 *  8.  Duplicate transaction detection (same requestId rejected)
 *  9.  Expired card rejection
 */
public class ATMApp {

    public static void main(String[] args) {

        // ------------------------------------------------------------------
        // Bootstrap — bank data
        // ------------------------------------------------------------------
        Bank bank = Bank.getInstance();

        BankAccount alice   = new BankAccount("ACC001", "Alice",   10000.0, "1234");
        BankAccount bob     = new BankAccount("ACC002", "Bob",       500.0, "5678");
        BankAccount charlie = new BankAccount("ACC003", "Charlie",  5000.0, "9999");

        bank.addAccount(alice);
        bank.addAccount(bob);
        bank.addAccount(charlie);

        bank.addCard(new Card("CARD001", "ACC001", LocalDate.now().plusYears(3)));  // Alice  — active
        bank.addCard(new Card("CARD002", "ACC002", LocalDate.now().plusYears(2)));  // Bob    — active
        bank.addCard(new Card("CARD003", "ACC003", LocalDate.now().plusYears(1)));  // Charlie— active (will get blocked)
        bank.addCard(new Card("CARD004", "ACC001", LocalDate.now().minusDays(1))); // Alice  — expired

        // ATM1 — loaded with multiple denominations (total Rs.17,000)
        CashDispenser dispenser1 = new CashDispenser();
        Map<Integer, Integer> load1 = new HashMap<>();
        load1.put(2000, 5);   // Rs.10,000
        load1.put(500,  10);  // Rs. 5,000
        load1.put(100,  20);  // Rs. 2,000
        dispenser1.loadCash(load1);
        ATM atm1 = new ATM("ATM-001", dispenser1, bank);

        // ATM2 — only Rs.500 notes (for rollback scenario)
        CashDispenser dispenser2 = new CashDispenser();
        Map<Integer, Integer> load2 = new HashMap<>();
        load2.put(500, 10);   // Rs. 5,000 total, only 500-rupee notes
        dispenser2.loadCash(load2);
        ATM atm2 = new ATM("ATM-002", dispenser2, bank);

        separator("Scenario 1 — Successful Balance Enquiry");
        atm1.insertCard("CARD001");
        atm1.authenticatePin("1234");
        atm1.checkBalance();
        atm1.ejectCard();

        // Alice balance: Rs.10,000 | ATM-001: Rs.17,000
        separator("Scenario 2 — Successful Withdrawal");
        atm1.insertCard("CARD001");
        atm1.authenticatePin("1234");
        atm1.withdrawCash(3000, null);
        atm1.ejectCard();
        System.out.println("Alice's balance after withdrawal: Rs." + alice.getBalance());

        // Alice balance: Rs.7,000 | ATM-001: Rs.14,000
        separator("Scenario 3 — Invalid PIN (3 attempts → card blocked)");
        atm1.insertCard("CARD003");
        tryInvalidPin(atm1, "0000");   // failedAttempts = 1
        tryInvalidPin(atm1, "1111");   // failedAttempts = 2
        tryInvalidPin(atm1, "2222");   // failedAttempts = 3 → account locked, card blocked & ejected

        separator("Scenario 4 — Attempting to Use a Blocked Card");
        tryBlockedCard(atm1, "CARD003");

        separator("Scenario 5 — Insufficient Account Balance");
        atm1.insertCard("CARD002");
        atm1.authenticatePin("5678");
        tryWithdraw(atm1, 2000, null);   // Bob has only Rs.500
        atm1.ejectCard();
        System.out.println("Bob's balance (unchanged): Rs." + bob.getBalance());

        // ATM-001 cash: ~Rs.14,000
        separator("Scenario 6 — Insufficient ATM Cash");
        atm1.insertCard("CARD001");
        atm1.authenticatePin("1234");
        tryWithdraw(atm1, 50000, null);   // ATM only has ~Rs.14,000
        atm1.ejectCard();
        System.out.println("Alice's balance (unchanged): Rs." + alice.getBalance());

        separator("Scenario 7 — Transaction ROLLBACK (can't make exact change)");
        // ATM-002 only has Rs.500 notes; requesting Rs.750 → greedy gives Rs.500, remaining Rs.250 → fail → rollback
        atm2.insertCard("CARD001");
        atm2.authenticatePin("1234");
        double balanceBefore = alice.getBalance();
        System.out.println("Alice's balance BEFORE rollback attempt: Rs." + balanceBefore);
        tryWithdraw(atm2, 750, null);
        double balanceAfter = alice.getBalance();
        System.out.println("Alice's balance AFTER  rollback attempt: Rs." + balanceAfter);
        System.out.println("Rollback correct (balances match): " + (balanceBefore == balanceAfter));
        atm2.ejectCard();

        separator("Scenario 8 — Duplicate Transaction Detection");
        atm1.insertCard("CARD001");
        atm1.authenticatePin("1234");
        String requestId = UUID.randomUUID().toString();
        System.out.println("First request  (requestId=" + requestId.substring(0, 8) + "...):");
        atm1.withdrawCash(500, requestId);
        System.out.println("Second request (same requestId):");
        tryWithdraw(atm1, 500, requestId);
        atm1.ejectCard();
        System.out.println("Alice's balance after duplicate test: Rs." + alice.getBalance());

        separator("Scenario 9 — Expired Card Rejection");
        tryExpiredCard(atm1, "CARD004");
    }

    // ------------------------------------------------------------------
    // Helper wrappers — catch & print expected exceptions gracefully
    // ------------------------------------------------------------------

    private static void tryInvalidPin(ATM atm, String pin) {
        try {
            atm.authenticatePin(pin);
        } catch (InvalidPinException | CardBlockedException e) {
            System.out.println("[EXPECTED ERROR] " + e.getMessage());
        }
    }

    private static void tryBlockedCard(ATM atm, String cardNumber) {
        try {
            atm.insertCard(cardNumber);
        } catch (CardBlockedException e) {
            System.out.println("[EXPECTED ERROR] " + e.getMessage());
        }
    }

    private static void tryExpiredCard(ATM atm, String cardNumber) {
        try {
            atm.insertCard(cardNumber);
        } catch (CardBlockedException e) {
            System.out.println("[EXPECTED ERROR] " + e.getMessage());
        }
    }

    private static void tryWithdraw(ATM atm, int amount, String requestId) {
        try {
            atm.withdrawCash(amount, requestId);
        } catch (InsufficientBalanceException | InsufficientCashException | DuplicateTransactionException e) {
            System.out.println("[EXPECTED ERROR] " + e.getMessage());
        }
    }

    private static void separator(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));
    }
}
