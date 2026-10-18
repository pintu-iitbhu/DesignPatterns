# Accounting Engine — LLD Interview Guide

## Problem Statement

Design a configurable, extensible **Accounting Engine** that can evaluate financial transactions against a set of business rules, generate double-entry ledger records, and produce a risk score and outcome.

---

## Functional Requirements

| # | Requirement |
|---|---|
| 1 | Create, update, enable, and disable rules at runtime |
| 2 | Match rules against a transaction by priority order |
| 3 | Support conditions on **amount, currency, payment method, merchant, and transaction status** |
| 4 | Support condition operators: **EQUALS, GREATER_THAN, BETWEEN, IN, AND, OR, NOT** |
| 5 | Generate debit and credit **ledger entries** from rule actions |
| 6 | Validate that **total debits == total credits** (double-entry bookkeeping) |
| 7 | **Prevent duplicate processing** of the same transaction event (idempotency) |
| 8 | Store transaction results and a full **audit trail** |
| 9 | Support **refunds and reversals** (mirror original ledger entries with flipped types) |
| 10 | Return a clear `EngineStatus` when no rule matches or a rule fails |

---

## Non-Functional Requirements

- Rules must be configurable without redeploying (runtime CRUD).
- Engine must be **extensible**: adding new condition types or action types requires zero changes to the engine core.
- Processing of the same event ID must be **idempotent** — safe to retry.
- Audit log must be **append-only**.
- Ledger entries are only persisted when the full transaction is **APPROVED** and balanced.

---

## Key Interview Questions

### 1. How do you make conditions extensible without modifying the engine?
`RuleCondition` is a `@FunctionalInterface`. Any new condition is a class that implements one method. The engine calls `condition.evaluate(tx)` — it never knows or cares what the condition checks. Adding `CurrencyCondition`, `PaymentMethodCondition`, or a future `GeoCondition` requires **zero changes** to the engine.

### 2. How do you support AND / OR / NOT composites?
Two approaches coexist:
- **Default combinators** on `RuleCondition` (`and()`, `or()`, `negate()`) work as inline Decorators — pure function composition.
- **`CompositeCondition`** handles named `AND / OR / NOT` with a list of child conditions — Composite pattern.

### 3. How do you enforce rule priority?
`Rule` carries an `int priority` (lower = higher priority). Before evaluation, the engine sorts `findAllEnabled()` results with `Comparator.comparingInt(Rule::priority)`. This is O(n log n) and requires no change when new rules are added.

### 4. How do you prevent double-processing (idempotency)?
`ProcessedEventStore` (backed by `InMemoryProcessedEventStore` / can be Redis in prod) maps `transactionId → EngineResult`. Before any evaluation, `getResult(id)` is checked. On a hit, the cached result is returned immediately and `DUPLICATE_DETECTED` is written to the audit log.

### 5. How do you ensure ledger balance (debit = credit)?
`LedgerValidator.validate(entries)` sums all DEBIT amounts and all CREDIT amounts using streams. If they differ, `LedgerImbalanceException` is thrown, the engine sets status to `VALIDATION_FAILED`, and entries are **not** persisted to `LedgerRepository`.

### 6. How do you support reversals?
`ReversalAction` reads `metadata["originalTransactionId"]`, fetches the original `LedgerEntry` list from `LedgerRepository`, and creates mirrored entries (DEBIT↔CREDIT swapped). Since reversal entries are balanced, they pass validation and persist normally.

### 7. How does the audit trail work?
`AuditRepository` is append-only. `DefaultAccountingEngine` calls `audit(...)` at every significant step:
`PROCESSING_STARTED → RULE_MATCHED / RULE_SKIPPED → VALIDATION_FAILED? → DUPLICATE_DETECTED? → PROCESSING_COMPLETE`

### 8. What happens when no rule matches?
`anyRuleMatched` flag is tracked in the loop. If false, `resolveStatus()` returns `EngineStatus.NO_RULE_MATCHED`. The result carries an empty score, no flags, and `approved() == false`.

### 9. How do you support new action types?
Implement `RuleAction`:
```java
public interface RuleAction {
    ActionResult execute(Transaction transaction, EvaluationContext context);
}
```
`ActionResult` carries `actionType`, `scoreDelta`, and `message`. The engine records this in `RuleResult` — no engine change needed.

### 10. How would you scale this to a distributed system?
- Replace `InMemoryProcessedEventStore` with a **Redis** or **DynamoDB** backed store (same interface).
- Replace `InMemoryRuleRepository` with a **DB-backed** one with caching (e.g. Caffeine + DB).
- Replace `InMemoryLedgerRepository` with a **transactional DB** (PostgreSQL with ACID guarantees).
- The `AuditRepository` becomes an **event stream** (Kafka topic).
- The engine itself is stateless — horizontally scalable.

---

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `RuleCondition`, `RuleAction` implementations | Swap condition/action logic without changing callers |
| **Decorator** | `RuleCondition.and()` / `.or()` / `.negate()` default methods | Compose conditions inline without subclassing |
| **Composite** | `CompositeCondition` (AND/OR/NOT), `ChainedAction` | Treat a group of conditions/actions as one |
| **Chain of Responsibility** | Priority-sorted rule loop in `DefaultAccountingEngine` | Each rule inspects the transaction; rejected result short-circuits |
| **Builder** | `RuleBuilder` | Fluent, validated construction of `Rule` records |
| **Repository** | `RuleRepository`, `LedgerRepository`, `AuditRepository` | Decouple storage from business logic; swap in-memory ↔ DB |
| **Context Object** | `EvaluationContext` | Mutable accumulator passed through all actions in a single evaluation |
| **Null Object / Status Enum** | `EngineStatus.NO_RULE_MATCHED` | Explicit no-match result instead of null or exception |

---

## Package Structure

```
src/main/java/com/rcpky/
│
├── domain/
│   ├── Transaction.java          ← record: id, accountId, merchantId, amount, currency,
│   │                                       type, paymentMethod, status, timestamp, metadata
│   ├── TransactionType.java      ← DEBIT | CREDIT | TRANSFER | REFUND | REVERSAL
│   ├── PaymentMethod.java        ← CARD | UPI | NET_BANKING | WALLET | CASH | BANK_TRANSFER
│   ├── TransactionStatus.java    ← PENDING | COMPLETED | FAILED | REVERSED
│   └── RiskLevel.java            ← LOW | MEDIUM | HIGH | CRITICAL
│
├── rule/
│   ├── RuleCondition.java        ← @FunctionalInterface + and/or/negate combinators
│   ├── RuleAction.java           ← interface: execute(tx, ctx) → ActionResult
│   ├── ActionResult.java         ← record: actionType, scoreDelta, message
│   └── Rule.java                 ← record: id, name, priority, condition, action, enabled
│
├── condition/
│   ├── AmountCondition.java      ← GT, LT, GTE, LTE, EQ, BETWEEN on BigDecimal
│   ├── TransactionTypeCondition  ← IN set of TransactionType
│   ├── MerchantCondition         ← IN set of merchantId strings
│   ├── CurrencyCondition         ← IN set of ISO currency codes
│   ├── PaymentMethodCondition    ← IN set of PaymentMethod
│   ├── TransactionStatusCondition← IN set of TransactionStatus
│   └── CompositeCondition        ← AND | OR | NOT over child conditions
│
├── action/
│   ├── ScoreAction               ← adds scoreDelta to context
│   ├── FlagAction                ← adds a named flag + score
│   ├── CategoryAction            ← tags transaction with a category
│   ├── RejectAction              ← marks context rejected, short-circuits loop
│   ├── LedgerAction              ← generates LedgerEntry list via Function<Tx, List<Entry>>
│   ├── ReversalAction            ← mirrors original tx's ledger entries (DEBIT↔CREDIT)
│   └── ChainedAction             ← executes multiple actions as one (Composite)
│
├── engine/
│   ├── AccountingEngine.java     ← interface
│   ├── DefaultAccountingEngine   ← orchestrator: idempotency → rules → ledger validation → audit
│   ├── EvaluationContext         ← mutable per-evaluation accumulator
│   ├── EngineResult.java         ← record: transactionId, status, score, riskLevel, flags,
│   │                                        categories, ledgerEntries, ruleResults, rejectionReason
│   ├── RuleResult.java           ← record: per-rule outcome
│   ├── EngineStatus.java         ← APPROVED | REJECTED | NO_RULE_MATCHED | DUPLICATE | VALIDATION_FAILED
│   ├── ScoreCalculator           ← score → RiskLevel (0-29 LOW, 30-59 MEDIUM, 60-84 HIGH, 85+ CRITICAL)
│   └── LedgerValidator           ← asserts Σ DEBIT == Σ CREDIT
│
├── ledger/
│   ├── LedgerEntry.java          ← record: entryId, transactionId, accountId, type, amount, …
│   ├── LedgerEntryType.java      ← DEBIT | CREDIT
│   ├── LedgerRepository.java     ← interface
│   └── InMemoryLedgerRepository  ← ConcurrentHashMap<txId, List<LedgerEntry>>
│
├── audit/
│   ├── AuditEvent.java           ← record: id, transactionId, type, message, timestamp
│   ├── AuditEventType.java       ← PROCESSING_STARTED | RULE_MATCHED | RULE_SKIPPED |
│   │                                PROCESSING_COMPLETE | DUPLICATE_DETECTED | VALIDATION_FAILED
│   ├── AuditRepository.java      ← interface
│   └── InMemoryAuditRepository   ← append-only ConcurrentHashMap<txId, List<AuditEvent>>
│
├── idempotency/
│   ├── ProcessedEventStore.java  ← interface: isProcessed / markProcessed / getResult
│   └── InMemoryProcessedEventStore ← ConcurrentHashMap<txId, EngineResult>
│
├── repository/
│   ├── RuleRepository.java       ← interface: save / findById / findAllEnabled / delete / update
│   └── InMemoryRuleRepository    ← ConcurrentHashMap<id, Rule>
│
├── builder/
│   └── RuleBuilder.java          ← fluent DSL: id / name / priority / when / then / disabled / build
│
└── exception/
    ├── RuleNotFoundException
    └── LedgerImbalanceException
```

---

## Evaluation Flow

```
evaluate(tx)
    │
    ├─ ProcessedEventStore.getResult(tx.id)
    │       hit?  ──► return cached result  [DUPLICATE_DETECTED audit]
    │       miss? ──► continue
    │
    ├─ [PROCESSING_STARTED audit]
    │
    ├─ RuleRepository.findAllEnabled()
    │       sort by Rule.priority (ascending)
    │
    ├─ for each Rule (in priority order):
    │       condition.evaluate(tx)
    │           true  ──► action.execute(tx, ctx)  [RULE_MATCHED audit]
    │                       ctx.isRejected? ──► break
    │           false ──► [RULE_SKIPPED audit]
    │
    ├─ resolveStatus(anyRuleMatched, ctx)
    │       no match  ──► NO_RULE_MATCHED
    │       rejected  ──► REJECTED
    │       else      ──► APPROVED
    │
    ├─ if APPROVED and ledgerEntries exist:
    │       LedgerValidator.validate(entries)
    │           balanced?   ──► LedgerRepository.saveAll(entries)
    │           imbalanced? ──► status = VALIDATION_FAILED  [VALIDATION_FAILED audit]
    │
    ├─ build EngineResult(transactionId, status, score, riskLevel, …)
    ├─ ProcessedEventStore.markProcessed(tx.id, result)
    └─ [PROCESSING_COMPLETE audit]
       return result
```

---

## Running Tests

```bash
mvn test
```

**23 tests** across two suites:

| Suite | Tests | Covers |
|---|---|---|
| `AccountingEngineTest` | 13 | Risk scoring, priorities, composites, reject short-circuit, negate, BETWEEN, ChainedAction, disabled rules, dynamic add/remove |
| `LedgerEngineTest` | 10 | Balanced ledger, imbalance validation, idempotency, audit trail, no-match, currency/payment-method/status conditions, reversal, ledger persistence |
