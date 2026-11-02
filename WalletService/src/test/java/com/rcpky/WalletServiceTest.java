package com.rcpky;

import com.rcpky.exception.WalletException;
import com.rcpky.model.Transaction;
import com.rcpky.model.TransactionType;
import com.rcpky.model.Wallet;
import com.rcpky.repository.impl.InMemoryTransactionRepository;
import com.rcpky.repository.impl.InMemoryWalletRepository;
import com.rcpky.service.WalletService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class WalletServiceTest {

    private WalletService service;

    @Before
    public void setUp() {
        service = new WalletService(new InMemoryWalletRepository(), new InMemoryTransactionRepository());
    }

    /* 1. Create wallet - success */
    @Test
    public void createWallet_success() {
        Wallet w = service.createWallet("Alice", "USD", new BigDecimal("100.00"));
        assertNotNull(w.getId());
        assertEquals("Alice", w.getOwnerName());
        assertEquals("USD", w.getCurrency());
        assertEquals(0, new BigDecimal("100.00").compareTo(w.getBalance()));
    }

    /* 2. Create wallet - negative initial balance fails */
    @Test(expected = WalletException.class)
    public void createWallet_negativeBalance_throws() {
        service.createWallet("Bob", "USD", new BigDecimal("-10"));
    }

    /* 3. Create wallet - blank name fails */
    @Test(expected = WalletException.class)
    public void createWallet_blankName_throws() {
        service.createWallet("  ", "USD", BigDecimal.ZERO);
    }

    /* 4. Create wallet with zero balance - no initial transaction */
    @Test
    public void createWallet_zeroBalance_noTransaction() {
        Wallet w = service.createWallet("Carol", "EUR", BigDecimal.ZERO);
        List<Transaction> txns = service.getTransactionHistory(w.getId());
        assertTrue(txns.isEmpty());
    }

    /* 5. Create wallet with positive balance - initial CREDIT transaction */
    @Test
    public void createWallet_positiveBalance_creditTransaction() {
        Wallet w = service.createWallet("Dave", "GBP", new BigDecimal("50"));
        List<Transaction> txns = service.getTransactionHistory(w.getId());
        assertEquals(1, txns.size());
        assertEquals(TransactionType.CREDIT, txns.get(0).getType());
        assertEquals(0, new BigDecimal("50").compareTo(txns.get(0).getAmount()));
    }

    /* 6. Credit - balance increases */
    @Test
    public void credit_success() {
        Wallet w = service.createWallet("Eve", "USD", new BigDecimal("100"));
        Transaction t = service.credit(w.getId(), new BigDecimal("50"), "Salary", null);
        assertEquals(TransactionType.CREDIT, t.getType());
        assertEquals(0, new BigDecimal("150").compareTo(service.getWallet(w.getId()).getBalance()));
    }

    /* 7. Credit - zero or negative amount fails */
    @Test(expected = WalletException.class)
    public void credit_invalidAmount_throws() {
        Wallet w = service.createWallet("Frank", "USD", BigDecimal.ZERO);
        service.credit(w.getId(), BigDecimal.ZERO, "Bad credit", null);
    }

    /* 8. Debit - balance decreases */
    @Test
    public void debit_success() {
        Wallet w = service.createWallet("Grace", "USD", new BigDecimal("200"));
        Transaction t = service.debit(w.getId(), new BigDecimal("80"), "Groceries", null);
        assertEquals(TransactionType.DEBIT, t.getType());
        assertEquals(0, new BigDecimal("120").compareTo(service.getWallet(w.getId()).getBalance()));
    }

    /* 9. Debit - insufficient funds fails */
    @Test(expected = WalletException.class)
    public void debit_insufficientFunds_throws() {
        Wallet w = service.createWallet("Hank", "USD", new BigDecimal("10"));
        service.debit(w.getId(), new BigDecimal("20"), "Overdraft", null);
    }

    /* 10. Transfer - balances updated on both wallets */
    @Test
    public void transfer_success() {
        Wallet from = service.createWallet("Ivy", "USD", new BigDecimal("500"));
        Wallet to = service.createWallet("Jack", "USD", new BigDecimal("100"));
        List<Transaction> txns = service.transfer(from.getId(), to.getId(), new BigDecimal("200"), "Rent", null);

        assertEquals(2, txns.size());
        TransactionType outType = txns.stream().filter(t -> t.getWalletId().equals(from.getId())).findFirst().get().getType();
        TransactionType inType = txns.stream().filter(t -> t.getWalletId().equals(to.getId())).findFirst().get().getType();
        assertEquals(TransactionType.TRANSFER_OUT, outType);
        assertEquals(TransactionType.TRANSFER_IN, inType);

        assertEquals(0, new BigDecimal("300").compareTo(service.getWallet(from.getId()).getBalance()));
        assertEquals(0, new BigDecimal("300").compareTo(service.getWallet(to.getId()).getBalance()));
    }

    /* 11. Transfer - same wallet fails */
    @Test(expected = WalletException.class)
    public void transfer_sameWallet_throws() {
        Wallet w = service.createWallet("Kim", "USD", new BigDecimal("100"));
        service.transfer(w.getId(), w.getId(), new BigDecimal("10"), "Self", null);
    }

    /* 12. Transfer - insufficient funds fails */
    @Test(expected = WalletException.class)
    public void transfer_insufficientFunds_throws() {
        Wallet from = service.createWallet("Leo", "USD", new BigDecimal("10"));
        Wallet to = service.createWallet("Mia", "USD", BigDecimal.ZERO);
        service.transfer(from.getId(), to.getId(), new BigDecimal("50"), "Too much", null);
    }

    /* 13. Transaction history - returns newest first */
    @Test
    public void getTransactionHistory_orderedByTimestamp() throws InterruptedException {
        Wallet w = service.createWallet("Nina", "USD", new BigDecimal("100"));
        Thread.sleep(2);
        service.credit(w.getId(), new BigDecimal("50"), "First credit", null);
        Thread.sleep(2);
        service.debit(w.getId(), new BigDecimal("20"), "First debit", null);
        List<Transaction> txns = service.getTransactionHistory(w.getId());
        assertTrue(txns.size() >= 2);
        assertTrue(txns.get(0).getTimestamp().isAfter(txns.get(txns.size() - 1).getTimestamp())
                || txns.get(0).getTimestamp().equals(txns.get(txns.size() - 1).getTimestamp()));
    }

    /* 14. Get wallet - not found throws */
    @Test(expected = WalletException.class)
    public void getWallet_notFound_throws() {
        service.getWallet("non-existent-id");
    }

    /* 15. Get all wallets */
    @Test
    public void getAllWallets_returnsAll() {
        service.createWallet("Oscar", "USD", BigDecimal.ZERO);
        service.createWallet("Pam", "EUR", BigDecimal.ZERO);
        List<Wallet> all = service.getAllWallets();
        assertTrue(all.size() >= 2);
    }

    /* 16. Idempotency - duplicate credit with same key returns same transaction, balance unchanged */
    @Test
    public void credit_idempotentKey_returnsSameTransaction() {
        Wallet w = service.createWallet("Quinn", "USD", new BigDecimal("100"));
        Transaction t1 = service.credit(w.getId(), new BigDecimal("50"), "Bonus", "idem-credit-001");
        Transaction t2 = service.credit(w.getId(), new BigDecimal("50"), "Bonus", "idem-credit-001");
        assertEquals(t1.getId(), t2.getId());
        assertEquals(0, new BigDecimal("150").compareTo(service.getWallet(w.getId()).getBalance()));
    }

    /* 17. Idempotency - duplicate transfer with same key returns same transactions, balances unchanged */
    @Test
    public void transfer_idempotentKey_returnsSameTransactions() {
        Wallet from = service.createWallet("Ray", "USD", new BigDecimal("500"));
        Wallet to   = service.createWallet("Sam", "USD", new BigDecimal("100"));
        List<Transaction> r1 = service.transfer(from.getId(), to.getId(), new BigDecimal("100"), "Rent", "idem-transfer-001");
        List<Transaction> r2 = service.transfer(from.getId(), to.getId(), new BigDecimal("100"), "Rent", "idem-transfer-001");
        assertEquals(r1.get(0).getId(), r2.get(0).getId());
        assertEquals(0, new BigDecimal("400").compareTo(service.getWallet(from.getId()).getBalance()));
        assertEquals(0, new BigDecimal("200").compareTo(service.getWallet(to.getId()).getBalance()));
    }
}
