# Accounting Engine — LLD Interview Guide

## Problem Statement

Design a configurable, extensible **Accounting Engine** that can evaluate financial transactions against a set of business rules, generate double-entry ledger records, and produce a risk score and outcome.

---

## Before You Start: Clarifying Questions to Ask the Interviewer

> Ask these **before writing a single line of code**. They signal seniority, prevent wasted design, and often unlock hidden constraints that change the entire approach.

---

### 1. Transaction Model

> *"What does a transaction look like? What attributes does it carry?"*

- Does a transaction have a fixed schema (amount, currency, type) or is it a free-form key-value bag?
- What types of transactions do we need to support? (debit, credit, refund, reversal, transfer?)
- Is `amount` always positive, or can it be negative (for reversals)?
- Do transactions carry metadata for custom attributes (e.g., `deviceType`, `geoLocation`, `merchantCategory`)?
- Is `transactionId` guaranteed unique per event, or can the same logical transaction arrive multiple times (retries)?

**What this unlocks:** Whether `Transaction` is a record with typed fields or a generic `Map<String, Object>`, and whether you need idempotency handling at all.

---

### 2. Rules — Structure and Configuration

> *"What is a rule? How is it defined and who manages it?"*

- Is a rule just a condition + action, or does it have more structure (priority, groups, tags)?
- Who configures rules — engineers via code, or business users via a UI / config file / DB?
- Do rules need to be created, updated, and deleted **at runtime** without a redeploy?
- Can rules be **enabled/disabled** without deleting them (for A/B testing or rollback)?
- Should rule changes take effect **immediately** or only on the next deployment?
- Is there a maximum number of rules we should design for? (10s? 1000s?)

**What this unlocks:** Whether `RuleRepository` is in-memory (simple) or DB-backed with caching, and whether the engine needs a `ReadWriteLock` around rule reads.

---

### 3. Conditions — Complexity and Operators

> *"What can a condition check, and how complex can it be?"*

- Which transaction fields can conditions check? (amount, currency, merchant, payment method, type, status, metadata?)
- What operators are needed? (equals, greater than, less than, between, IN set, regex?)
- Do we need **composite conditions** — AND, OR, NOT across multiple sub-conditions?
- Can conditions be **nested** (e.g., `(A AND B) OR (C AND NOT D)`)?
- Should conditions be data-driven (stored as JSON in DB) or code-driven (Java classes)?

**What this unlocks:** Whether you need `CompositeCondition` + recursive condition trees, and whether you need a `ConditionSerializer` to persist conditions.

---

### 4. Actions — What Can a Rule Do?

> *"When a rule matches, what can it do to the transaction?"*

- Can a rule **score** a transaction (add a risk score)?
- Can a rule **flag** a transaction with a named label (e.g., `HIGH_VALUE`, `SUSPICIOUS`)?
- Can a rule **reject/block** a transaction immediately?
- Can a rule generate **ledger entries** (debit/credit accounting records)?
- Can a rule **categorize** a transaction (e.g., `INTERNATIONAL`, `LARGE_TRANSACTION`)?
- Can a rule trigger multiple actions (e.g., flag AND score AND categorize)?
- Should a rejection stop processing all remaining rules, or should all rules run regardless?

**What this unlocks:** The full set of `RuleAction` implementations needed, and whether `ChainedAction` (Composite pattern) is required.

---

### 5. Scoring and Risk

> *"How is the final score computed and what does it represent?"*

- Does every matched rule contribute a score delta, or does only the highest-priority match count?
- Can scores be negative (e.g., a rule that reduces risk)?
- Are risk levels required? (LOW / MEDIUM / HIGH / CRITICAL) What are the thresholds?
- Should the score be part of the returned result, or is it only used internally for decisions?
- Can a score threshold auto-reject a transaction? (e.g., score > 90 → auto-reject)

**What this unlocks:** Whether `ScoreCalculator` thresholds are hardcoded or configurable, and whether score-based auto-rejection is needed.

---

### 6. Ledger and Accounting

> *"Do we need to generate accounting entries from transactions?"*

- Is **double-entry bookkeeping** required? (every debit must have an equal credit)
- What does a ledger entry look like? (accountId, amount, type, description?)
- Should ledger entries only be persisted if the transaction is fully approved and balanced?
- What happens if ledger entries are **imbalanced** — fail the transaction or log and continue?
- Do we need to support **reversals/refunds** that mirror the original ledger entries?
- Are ledger entries append-only, or can they be corrected/amended?

**What this unlocks:** Whether `LedgerValidator` and `LedgerRepository` are needed, and whether `ReversalAction` is in scope.

---

### 7. Idempotency and Duplicate Handling

> *"What happens if the same transaction is submitted twice?"*

- Is the `transactionId` a globally unique idempotency key?
- Should re-processing the same ID return the **cached result** from the first run?
- Or should it be re-evaluated fresh each time?
- How long should processed events be remembered? (indefinitely? TTL?)
- What should be logged when a duplicate is detected?

**What this unlocks:** Whether `ProcessedEventStore` is needed, and whether it's in-memory (simple) or Redis-backed (distributed).

---

### 8. Audit Trail

> *"Do we need to record what happened during processing?"*

- Is an audit trail required? (which rules fired, which were skipped, final outcome?)
- Should the audit log be **append-only** (no updates or deletes)?
- Who consumes the audit trail — internal systems, compliance teams, end users?
- Does audit need to be queryable by `transactionId`? By time range? By rule?
- Should audit events be synchronous (blocking) or async (fire-and-forget)?

**What this unlocks:** Whether `AuditRepository` is append-only in-memory or a Kafka event stream, and the granularity of audit events.

---

### 9. Concurrency and Scale

> *"How many transactions per second do we need to handle?"*

- Is this a **single-node** in-process library or a **distributed service**?
- What is the expected TPS (transactions per second)? (1? 1000? 100,000?)
- Can multiple transactions be evaluated **concurrently** on the same engine instance?
- Can rules be **updated concurrently** while transactions are being processed?
- Is there a latency SLA per transaction evaluation? (e.g., < 5ms P99?)

**What this unlocks:** Whether you need `ConcurrentHashMap`, `ReadWriteLock` on rule updates, thread-pool sizing, or async processing entirely.

---

### 10. Extensibility and Future Growth

> *"How much flexibility do we need to add new rule types later?"*

- Will new condition types be added frequently? (e.g., `GeoCondition`, `VelocityCondition`, `MLScoreCondition`)
- Will new action types be added? (e.g., `WebhookAction`, `AlertAction`, `FreezeAccountAction`)
- Should the engine support **rule versioning** — so we can replay how a transaction was evaluated under old rules?
- Should conditions be **serializable to/from JSON** so they can be stored in a DB and loaded at runtime?
- Will different clients need different rule sets (multi-tenancy)?

**What this unlocks:** Whether `RuleCondition` as a `@FunctionalInterface` is enough, or if you need a serializable `ConditionSpec` with a `ConditionFactory`.

---

### 11. Error Handling

> *"What should happen when something goes wrong during evaluation?"*

- If a **rule condition throws an exception** (e.g., null pointer in a custom condition), should evaluation fail fast or skip that rule and continue?
- If a **rule action throws**, should the transaction be rejected or marked as errored?
- Should errors in one rule affect other rules in the pipeline?
- Is there a **dead-letter queue** for transactions that could not be processed?
- Should error details appear in the audit trail?

**What this unlocks:** Whether you wrap each rule evaluation in try-catch and what the failure `EngineStatus` values should be.

---

### 12. Output / API Contract

> *"What does the caller get back after evaluation?"*

- Is a simple pass/fail enough, or do callers need the full breakdown (which rules fired, final score, risk level, rejection reason)?
- Should `ledgerEntries` be returned in the result, or only persisted internally?
- Do callers need to query results later by `transactionId`?
- Is there a need for a **dry-run mode** — evaluate rules but don't persist any ledger entries or audit events?

**What this unlocks:** The shape of `EngineResult`, whether `getLedgerEntries(txId)` and `getAuditTrail(txId)` APIs are needed, and whether a dry-run flag is required.

---

### Quick Reference — Minimum Viable Clarifications

If the interviewer says *"just start building"*, lock down at least these five before touching code:

| # | Question | Why It Matters |
|---|---|---|
| 1 | What does a transaction look like? | Drives the entire domain model |
| 2 | What can a rule condition check? | Determines condition class hierarchy |
| 3 | What can a rule action do? | Determines action class hierarchy |
| 4 | Is double-entry ledger needed? | Major scope difference |
| 5 | Single node or distributed? | Determines all concurrency decisions |

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
