package com.rcpky.engine;

import com.rcpky.action.*;
import com.rcpky.audit.AuditEvent;
import com.rcpky.audit.AuditEventType;
import com.rcpky.audit.InMemoryAuditRepository;
import com.rcpky.builder.RuleBuilder;
import com.rcpky.condition.*;
import com.rcpky.domain.*;
import com.rcpky.idempotency.InMemoryProcessedEventStore;
import com.rcpky.ledger.InMemoryLedgerRepository;
import com.rcpky.ledger.LedgerEntry;
import com.rcpky.ledger.LedgerEntryType;
import com.rcpky.repository.InMemoryRuleRepository;
import com.rcpky.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LedgerEngineTest {

    private DefaultAccountingEngine engine;
    private RuleRepository repository;
    private InMemoryLedgerRepository ledgerRepository;
    private InMemoryAuditRepository auditRepository;
    private InMemoryProcessedEventStore processedEventStore;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRuleRepository();
        ledgerRepository = new InMemoryLedgerRepository();
        auditRepository = new InMemoryAuditRepository();
        processedEventStore = new InMemoryProcessedEventStore();
        engine = new DefaultAccountingEngine(repository, new LedgerValidator(),
                processedEventStore, auditRepository, ledgerRepository);
    }

    private Transaction tx(String id, BigDecimal amount, TransactionType type) {
        return new Transaction(id, "ACC001", "M001", amount, "USD", type,
                PaymentMethod.CARD, TransactionStatus.COMPLETED, Instant.now(), Map.of());
    }

    private Transaction txWithCurrency(String id, String currency) {
        return new Transaction(id, "ACC001", "M001", new BigDecimal("100"), currency,
                TransactionType.DEBIT, PaymentMethod.CARD, TransactionStatus.COMPLETED, Instant.now(), Map.of());
    }

    private Transaction txWithMethod(String id, PaymentMethod method) {
        return new Transaction(id, "ACC001", "M001", new BigDecimal("100"), "USD",
                TransactionType.DEBIT, method, TransactionStatus.COMPLETED, Instant.now(), Map.of());
    }

    private Transaction txWithStatus(String id, TransactionStatus status) {
        return new Transaction(id, "ACC001", "M001", new BigDecimal("100"), "USD",
                TransactionType.DEBIT, PaymentMethod.CARD, status, Instant.now(), Map.of());
    }

    // ── Ledger ──────────────────────────────────────────────────────────────

    @Test
    void ledgerActionShouldGenerateBalancedEntries() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Debit Ledger Rule").priority(1)
                .when(new TransactionTypeCondition(TransactionType.DEBIT))
                .then(new LedgerAction(t -> List.of(
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), t.accountId(),
                                LedgerEntryType.DEBIT, t.amount(), t.currency(), "Customer debit", Instant.now()),
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), "MERCHANT_ACC",
                                LedgerEntryType.CREDIT, t.amount(), t.currency(), "Merchant credit", Instant.now())
                )))
                .build());

        EngineResult result = engine.evaluate(tx("T001", new BigDecimal("500"), TransactionType.DEBIT));

        assertEquals(EngineStatus.APPROVED, result.status());
        assertEquals(2, result.ledgerEntries().size());
        assertEquals(2, ledgerRepository.findByTransactionId("T001").size());
    }

    @Test
    void imbalancedLedgerShouldYieldValidationFailedStatus() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Imbalanced Rule").priority(1)
                .when(new TransactionTypeCondition(TransactionType.DEBIT))
                .then(new LedgerAction(t -> List.of(
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), t.accountId(),
                                LedgerEntryType.DEBIT, t.amount(), t.currency(), "Only debit, no credit", Instant.now())
                )))
                .build());

        EngineResult result = engine.evaluate(tx("T002", new BigDecimal("500"), TransactionType.DEBIT));

        assertEquals(EngineStatus.VALIDATION_FAILED, result.status());
        assertFalse(result.approved());
        assertTrue(auditRepository.findByTransactionId("T002").stream()
                .anyMatch(e -> e.type() == AuditEventType.VALIDATION_FAILED));
    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    @Test
    void duplicateEventShouldReturnCachedResultWithoutReprocessing() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Score Rule").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, BigDecimal.ZERO))
                .then(new ScoreAction(10, "Scored"))
                .build());

        EngineResult first  = engine.evaluate(tx("T003", new BigDecimal("100"), TransactionType.DEBIT));
        EngineResult second = engine.evaluate(tx("T003", new BigDecimal("100"), TransactionType.DEBIT));

        assertEquals(first, second);
        assertTrue(auditRepository.findByTransactionId("T003").stream()
                .anyMatch(e -> e.type() == AuditEventType.DUPLICATE_DETECTED));
    }

    // ── Audit trail ──────────────────────────────────────────────────────────

    @Test
    void auditTrailShouldRecordStartAndCompleteEvents() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Test Rule").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, BigDecimal.ZERO))
                .then(new ScoreAction(10, "Scored"))
                .build());

        engine.evaluate(tx("T004", new BigDecimal("100"), TransactionType.DEBIT));
        List<AuditEvent> events = engine.getAuditTrail("T004");

        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> e.type() == AuditEventType.PROCESSING_STARTED));
        assertTrue(events.stream().anyMatch(e -> e.type() == AuditEventType.RULE_MATCHED));
        assertTrue(events.stream().anyMatch(e -> e.type() == AuditEventType.PROCESSING_COMPLETE));
    }

    // ── No-match ─────────────────────────────────────────────────────────────

    @Test
    void noRuleMatchedShouldReturnNoRuleMatchedStatus() {
        EngineResult result = engine.evaluate(tx("T005", new BigDecimal("100"), TransactionType.DEBIT));

        assertEquals(EngineStatus.NO_RULE_MATCHED, result.status());
        assertFalse(result.approved());
    }

    // ── New condition types ───────────────────────────────────────────────────

    @Test
    void currencyConditionShouldFilterByISOCode() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("USD/EUR Rule").priority(1)
                .when(new CurrencyCondition("USD", "EUR"))
                .then(new ScoreAction(5, "International currency"))
                .build());

        EngineResult usd = engine.evaluate(txWithCurrency("T006", "USD"));
        EngineResult inr = engine.evaluate(txWithCurrency("T007", "INR"));

        assertEquals(5, usd.totalScore());
        assertEquals(0, inr.totalScore());
        assertEquals(EngineStatus.NO_RULE_MATCHED, inr.status());
    }

    @Test
    void paymentMethodConditionShouldFilterByMethod() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Card Surcharge").priority(1)
                .when(new PaymentMethodCondition(PaymentMethod.CARD))
                .then(new ScoreAction(10, "Card surcharge"))
                .build());

        EngineResult card = engine.evaluate(txWithMethod("T008", PaymentMethod.CARD));
        EngineResult upi  = engine.evaluate(txWithMethod("T009", PaymentMethod.UPI));

        assertEquals(10, card.totalScore());
        assertEquals(0, upi.totalScore());
    }

    @Test
    void transactionStatusConditionShouldFilterByStatus() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Pending Hold").priority(1)
                .when(new TransactionStatusCondition(TransactionStatus.PENDING))
                .then(new FlagAction("HOLD", 20))
                .build());

        EngineResult pending   = engine.evaluate(txWithStatus("T010", TransactionStatus.PENDING));
        EngineResult completed = engine.evaluate(txWithStatus("T011", TransactionStatus.COMPLETED));

        assertTrue(pending.flags().contains("HOLD"));
        assertFalse(completed.flags().contains("HOLD"));
    }

    // ── Reversals ────────────────────────────────────────────────────────────

    @Test
    void reversalShouldCreateMirroredLedgerEntries() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Debit Ledger").priority(1)
                .when(new TransactionTypeCondition(TransactionType.DEBIT))
                .then(new LedgerAction(t -> List.of(
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), "CUSTOMER_ACC",
                                LedgerEntryType.DEBIT, t.amount(), t.currency(), "Purchase", Instant.now()),
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), "MERCHANT_ACC",
                                LedgerEntryType.CREDIT, t.amount(), t.currency(), "Purchase", Instant.now())
                )))
                .build());

        engine.addRule(RuleBuilder.builder()
                .id("R002").name("Reversal Ledger").priority(2)
                .when(new TransactionTypeCondition(TransactionType.REVERSAL))
                .then(new ReversalAction(ledgerRepository))
                .build());

        engine.evaluate(tx("ORIG", new BigDecimal("200"), TransactionType.DEBIT));

        Transaction reversal = new Transaction("REV001", "ACC001", "M001",
                new BigDecimal("200"), "USD", TransactionType.REVERSAL, PaymentMethod.CARD,
                TransactionStatus.PENDING, Instant.now(),
                Map.of("originalTransactionId", "ORIG"));

        EngineResult result = engine.evaluate(reversal);

        assertEquals(EngineStatus.APPROVED, result.status());
        assertEquals(2, result.ledgerEntries().size());
        assertTrue(result.ledgerEntries().stream()
                .anyMatch(e -> e.type() == LedgerEntryType.CREDIT && e.accountId().equals("CUSTOMER_ACC")));
        assertTrue(result.ledgerEntries().stream()
                .anyMatch(e -> e.type() == LedgerEntryType.DEBIT && e.accountId().equals("MERCHANT_ACC")));
    }

    // ── getLedgerEntries convenience ──────────────────────────────────────────

    @Test
    void getLedgerEntriesShouldReturnPersistedEntries() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Ledger Rule").priority(1)
                .when(new TransactionTypeCondition(TransactionType.CREDIT))
                .then(new LedgerAction(t -> List.of(
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), "BANK",
                                LedgerEntryType.DEBIT, t.amount(), t.currency(), "Bank debit", Instant.now()),
                        new LedgerEntry(UUID.randomUUID().toString(), t.id(), "ACC001",
                                LedgerEntryType.CREDIT, t.amount(), t.currency(), "Customer credit", Instant.now())
                )))
                .build());

        engine.evaluate(tx("T012", new BigDecimal("300"), TransactionType.CREDIT));

        List<LedgerEntry> entries = engine.getLedgerEntries("T012");
        assertEquals(2, entries.size());
    }
}
