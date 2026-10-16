# Feature Flag Service

An in-process Java library that an application embeds directly. No network calls, no external process — flag evaluation happens in the same JVM with nanosecond latency.

---

## Three Core Capabilities

| Capability | What it does |
|---|---|
| **Flag definitions** | Each flag has a name, a type (`BOOLEAN`, `STRING`, `INTEGER`), and a default value |
| **Targeting rules** | Evaluate context attributes against conditions using AND/OR logic; return a specific value on first match |
| **Percentage rollout** | If no rule matches, roll out to N% of users with sticky bucketing — same user always gets the same result, independent per flag |

---

## Project Structure

```
src/main/java/com/rcpky/
├── api/
│   └── FeatureFlagClient.java       ← public entry point (embed this)
├── model/
│   ├── FlagDefinition.java          ← name + type + defaultValue + rules + rollout
│   ├── EvaluationContext.java       ← userId + attribute map
│   ├── EvaluationResult.java        ← value + EvaluationReason
│   ├── EvaluationReason.java        ← RULE_MATCH | ROLLOUT | DEFAULT | FLAG_NOT_FOUND
│   ├── Rule.java                    ← logicOperator + conditions + targetValue
│   ├── Condition.java               ← attribute + operator + value
│   ├── Rollout.java                 ← percentage (0–100) + value
│   ├── FlagType.java                ← BOOLEAN | STRING | INTEGER
│   ├── LogicOperator.java           ← AND | OR
│   └── ConditionOperator.java       ← EQUALS, NOT_EQUALS, IN, NOT_IN, CONTAINS, GT, LT, GTE, LTE
├── engine/
│   ├── FlagEvaluator.java           ← orchestrates: rules → rollout → default
│   ├── RuleEvaluator.java           ← per-rule AND/OR condition matching
│   └── RolloutEvaluator.java        ← CRC32(flagName|userId) % 100 sticky bucketing
├── store/
│   ├── FlagStore.java               ← interface (swap for DB-backed impl)
│   └── InMemoryFlagStore.java       ← ConcurrentHashMap-backed
└── exception/
    ├── FlagNotFoundException.java
    └── TypeMismatchException.java
```

---

## Quick Start

```java
FeatureFlagClient client = new FeatureFlagClient();

// Register a boolean flag with a targeting rule + 20% rollout
client.register(new FlagDefinition(
    "dark-mode",
    FlagType.BOOLEAN,
    false,                          // default
    List.of(
        new Rule(
            LogicOperator.AND,
            List.of(new Condition("plan", ConditionOperator.IN, List.of("pro", "enterprise"))),
            true                    // return true if rule matches
        )
    ),
    new Rollout(20, true)           // 20% of remaining users get true
));

EvaluationContext ctx = EvaluationContext.of("user-42", Map.of("plan", "free"));
boolean enabled = client.getBooleanFlag("dark-mode", ctx);

// Get the full result with reason
EvaluationResult<Boolean> result = client.evaluate("dark-mode", ctx, Boolean.class);
// result.value()  → true/false
// result.reason() → RULE_MATCH | ROLLOUT | DEFAULT
```

---

## Evaluation Flow

Every call goes through one linear decision chain:

```
getBooleanFlag("dark-mode", ctx)
        │
        ▼
 FlagEvaluator.evaluate()
        │
        ├─► 1. Lookup flag in FlagStore
        │         └─ not found → FlagNotFoundException
        │
        ├─► 2. Type check (BOOLEAN flag vs Boolean.class requested)
        │         └─ mismatch → TypeMismatchException
        │
        ├─► 3. RuleEvaluator — iterate rules in order
        │         └─ first rule that matches → return targetValue  [RULE_MATCH]
        │
        ├─► 4. RolloutEvaluator — CRC32(flagName|userId) % 100
        │         └─ bucket < percentage → return rollout value    [ROLLOUT]
        │
        └─► 5. return defaultValue                                 [DEFAULT]
```

**Priority**: Targeting rules always beat rollout. Rollout only runs if no rule matched.

---

## Key Design Decisions

### Immutable records for model classes
`FlagDefinition`, `Rule`, `Condition` are Java records — immutable by construction. In a multithreaded embedded library, mutable model objects would require synchronisation everywhere a flag could be read while being updated. Records eliminate the problem entirely: once a reader holds a reference, the object cannot change under it.

### First-match-wins for rules
Rules are evaluated in order; the first matching rule's `targetValue` is returned immediately. This is the same model used by nginx location blocks and AWS WAF rules.
- **Advantage**: simple mental model — put the most specific rule first.
- **Trade-off**: rule order matters; a misconfigured order silently returns the wrong value.

### AND/OR logic — flat DNF model
Each `Rule` holds a flat `List<Condition>` combined with a single `LogicOperator` (AND or OR). This is Disjunctive Normal Form — sufficient for 95% of real-world flag rules and far simpler to configure and debug than a full expression tree.

For deeply nested logic like `(A AND B) OR (C AND D)`, two separate rules achieve the same result with first-match-wins semantics.

### CRC32 for sticky bucketing — not `hashCode()`

```
bucket = CRC32( flagName + "|" + userId ) % 100
if bucket < rollout.percentage() → user is in rollout
```

- `String.hashCode()` is **not stable across JVM restarts** — the JVM spec does not require it, and Java 9+ randomizes string hash seeds. The same user could fall in or out of rollout on each restart.
- **CRC32** is deterministic across all JVM versions and vendors, ships in the standard library, and is fast (hardware-accelerated on modern CPUs).
- Including `flagName` in the key means each flag rolls out **independently** — user `alice` may be in the 20% rollout for `flag-A` but not `flag-B`.

### `FlagStore` as an interface
`FlagEvaluator` depends on the `FlagStore` interface, not `InMemoryFlagStore`. Swap the backing store (database, remote config, file-based) without touching the engine. Tests inject fakes freely.

### Thread safety
| Component | Why it's safe |
|---|---|
| `InMemoryFlagStore` | `ConcurrentHashMap` — lock-free reads, atomic reference replacement on writes |
| `FlagEvaluator`, `RuleEvaluator` | Zero mutable state; all inputs passed as parameters |
| `RolloutEvaluator` | New `CRC32` instance per call — `CRC32` is not thread-safe so it must not be shared |
| `FlagDefinition`, `Rule`, `Condition` | Immutable records — safe to share across threads without synchronisation |

---

## Supported Condition Operators

| Operator | Example use |
|---|---|
| `EQUALS` | `country == "US"` |
| `NOT_EQUALS` | `plan != "free"` |
| `IN` | `country in ["US", "CA", "UK"]` |
| `NOT_IN` | `country not in ["CN", "RU"]` |
| `CONTAINS` | `email contains "@company.com"` |
| `GREATER_THAN` | `age > 18` |
| `LESS_THAN` | `score < 100` |
| `GREATER_THAN_OR_EQUAL` | `score >= 50` |
| `LESS_THAN_OR_EQUAL` | `retries <= 3` |

---

## Test Coverage (52 tests)

| Test class | What it covers |
|---|---|
| `RuleEvaluatorTest` | AND/OR logic, all operators, missing attributes, first-match semantics |
| `RolloutEvaluatorTest` | 0%/100% edge cases, stickiness across repeated calls, distribution uniformity, independence per flag |
| `FlagEvaluatorTest` | Rule-match priority over rollout, type validation, exception paths, all three flag types |
| `FeatureFlagClientTest` | End-to-end boolean/string/integer flags, complex AND+OR rules, anonymous context, deregister |

Run tests:
```bash
mvn test
```

---

## Interview Q&A

### Design & Architecture

**Q: Why is this a library and not a microservice?**
A library has zero network latency, no serialisation overhead, and no single point of failure. Flag evaluation is synchronous and in-process. The downside: every app instance loads its own flags; there is no central real-time view of evaluations. A microservice gives central management but adds a network hop to every flag check — unacceptable for hot paths. LaunchDarkly's SDK solves this by streaming flag updates into an in-process cache, giving the best of both.

**Q: How would you update flags at runtime without restarting?**
Three options:
1. **Polling** — a background thread calls `store.put()` every N seconds with refreshed definitions. Simplest.
2. **SSE/WebSocket push** — a config server pushes diffs; the store applies them on receipt. Lower latency.
3. **File watch** — watch a JSON config file with `WatchService`; reload on change. Works for local/staging.

The `FlagStore` interface already supports this — call `store.put()` with a new `FlagDefinition` and the next evaluation sees the update immediately via `ConcurrentHashMap`.

**Q: Why does `FlagEvaluator` depend on `FlagStore` interface and not `InMemoryFlagStore` directly?**
Dependency Inversion. `FlagEvaluator` states *what it needs*, not *how it's implemented*. You can swap `InMemoryFlagStore` for a `RedisBackedFlagStore` or `JdbcFlagStore` without touching the engine. Tests also inject fakes freely.

**Q: Why records for model classes?**
Records enforce immutability — once constructed, no one can change a rule's conditions or a flag's default value mid-evaluation. This is critical in a multithreaded embedded library. Mutable model classes would require synchronisation everywhere reads and writes could race.

---

### Targeting Rules

**Q: Why first-match-wins instead of scoring all rules?**
Simple mental model: put the most specific rule first. Scoring all rules and picking the highest is complex to configure and debug. First-match-wins is how nginx, AWS WAF, and most flag systems work. The trade-off: rule order matters — misconfigured order silently returns the wrong value.

**Q: How would you support nested AND/OR like `(A AND B) OR (C AND D)`?**
Replace `List<Condition>` with a recursive sealed interface:
```java
sealed interface RuleNode permits ConditionNode, GroupNode {}
record ConditionNode(Condition condition) implements RuleNode {}
record GroupNode(LogicOperator operator, List<RuleNode> children) implements RuleNode {}
```
`RuleEvaluator` becomes recursive. The current flat model covers 95% of real-world cases and is much simpler to reason about.

**Q: What if `contextValue` and `conditionValue` are different types in `GREATER_THAN`?**
`Integer.compareTo` expects an `Integer` — passing a `Long` throws `ClassCastException`. Fix: normalize numeric types to `Double` before comparing:
```java
if (a instanceof Number na && b instanceof Number nb) {
    return Double.compare(na.doubleValue(), nb.doubleValue());
}
```

---

### Sticky Bucketing

**Q: Why CRC32 and not `String.hashCode()` or MD5?**
- `hashCode()` — the JVM spec does not guarantee consistency across restarts. Java 9+ randomizes string hash seeds. The same user could flip in/out of rollout on each restart.
- MD5 — cryptographically stronger than needed, slower, requires `MessageDigest` boilerplate.
- CRC32 — deterministic across all JVM versions/vendors, in the standard library, fast (hardware-accelerated).

**Q: If rollout changes from 20% → 50%, are the new 50% a strict superset of the original 20%?**
Yes — with `CRC32(flagName|userId) % 100`, users with bucket 0–19 were in the 20%; users with bucket 0–49 are in the 50%. The original 20% is preserved inside the new 50%. The problem arises only if the flag name changes or a different hash seed is introduced — then everyone re-buckets.

**Q: How would you implement guaranteed monotonic inclusion across percentage changes?**
Assign each user a permanent random bucket (0–99) stored in a database on first encounter. On each evaluation, look up their stored bucket and compare to the threshold. The bucket never changes — rollout is guaranteed monotonically additive. This is what LaunchDarkly calls "consistent bucketing with a persistent assignment store."

**Q: What if `userId` is null for a percentage rollout?**
We return `Optional.empty()` and fall through to the default value. The rationale: anonymous users have no stable identity, so assigning a random bucket would flip on every evaluation. The safest behaviour is the default. Alternatively, pass a session-scoped random ID as the `userId`.

---

### Concurrency & Thread Safety

**Q: Walk through why `FeatureFlagClient` is thread-safe.**
- `InMemoryFlagStore` uses `ConcurrentHashMap` — lock-free reads, atomic reference replacement on writes.
- `FlagEvaluator` and `RuleEvaluator` hold zero mutable state; all inputs are parameters.
- `RolloutEvaluator` creates a new `CRC32` per call — `CRC32` is not thread-safe and must not be shared.
- `FlagDefinition`, `Rule`, `Condition` are immutable records — safe to share across threads without synchronisation.

**Q: What if `put()` and `evaluate()` race on the same flag?**
`ConcurrentHashMap.put()` atomically replaces the entire `FlagDefinition` reference. A concurrent reader sees either the old or the new definition — never a torn write. Because `FlagDefinition` is an immutable record, once a reader holds a reference, that object cannot change under it.

---

### Scalability & Performance

**Q: Time complexity of flag evaluation?**
O(R × C) — R rules, C conditions per rule. In practice R < 10 and C < 5, so this is effectively O(1). `allMatch`/`anyMatch` short-circuit reduce the average cost further.

**Q: 10,000 flags, 100k req/sec — what are the bottlenecks?**
`ConcurrentHashMap.get()` is O(1) amortized and negligible at this rate. The real concern is rule evaluation CPU if flags have many complex rules. A compiled evaluator (pre-compiling rules into decision trees) would be needed at extreme scale. Per-flag evaluation metrics would identify hot flags.

**Q: How would you add caching on top of the store?**
Decorator pattern — wrap any `FlagStore`:
```java
public class CachingFlagStore implements FlagStore {
    private final FlagStore delegate;
    private final Map<String, FlagDefinition> cache = new ConcurrentHashMap<>();

    public Optional<FlagDefinition> get(String name) {
        return Optional.ofNullable(cache.computeIfAbsent(name,
            k -> delegate.get(k).orElse(null)));
    }
    // put() must also invalidate the cache entry
}
```
The engine doesn't change — it still calls `store.get()`.

---

### Extensibility

**Q: How to add a `DOUBLE` flag type?**
1. Add `DOUBLE` to `FlagType`.
2. Add `case DOUBLE -> requested == Double.class` to `validateType()` in `FlagEvaluator`.
3. Add `getDoubleFlag()` on `FeatureFlagClient`.

The compiler's exhaustive `switch` will error on step 2 immediately if you forget it — no silent bugs.

**Q: How would you add an audit log?**
`EvaluationResult` already carries the reason. Add an observer hook:
```java
public interface EvaluationListener {
    void onEvaluation(String flagName, String userId, EvaluationResult<?> result);
}
```
`FlagEvaluator` fires registered listeners after every evaluation. Zero change to evaluation logic.

---

### Real-world Gaps

**No persistence — flags lost on restart.**
Back `FlagStore` with a database or load from a JSON config file on startup. The interface makes this a drop-in replacement.

**No type validation at registration time.**
Add to `FlagDefinition`'s compact constructor:
```java
boolean valid = switch (type) {
    case BOOLEAN -> defaultValue instanceof Boolean;
    case STRING  -> defaultValue instanceof String;
    case INTEGER -> defaultValue instanceof Integer;
};
if (!valid) throw new IllegalArgumentException("defaultValue type does not match FlagType");
```

**No kill-switch.**
Add an `enabled` boolean to `FlagDefinition`. If `false`, `FlagEvaluator` returns `defaultValue` immediately without evaluating rules or rollout.

**No metrics/observability.**
Add `AtomicLong` counters per `EvaluationReason` per flag, or integrate a `MeterRegistry`. Expose them via `client.getStats()`.

---

## Deep-Dive Interview Q&A

---

### Concurrency & Thread Safety

**Q: Walk me through every shared mutable state in this library. How is each protected?**
There is exactly one piece of shared mutable state: the `ConcurrentHashMap` inside `InMemoryFlagStore`. Everything else is either immutable (`FlagDefinition`, `Rule`, `Condition` are records) or stateless (`FlagEvaluator`, `RuleEvaluator` hold no fields). `ConcurrentHashMap` provides lock-free reads and segment-level locking on writes, so concurrent flag lookups never block each other.

**Q: Why is `CRC32` not thread-safe? What would go wrong if you shared one instance?**
`CRC32` maintains an internal `value` field that is mutated by every `update()` call. If two threads called `update()` simultaneously, they would interleave writes to that field, corrupting both checksums. The result would be wrong bucket assignments — some users could randomly flip in or out of rollout. Fix: create a new `CRC32` per call (current approach), or use a `ThreadLocal<CRC32>` to reuse instances per thread without contention.

**Q: What's the difference between `HashMap` and `ConcurrentHashMap` under concurrent reads and writes?**
`HashMap` provides no thread-safety guarantees — concurrent modification can cause infinite loops (Java 7), lost updates, or `ConcurrentModificationException`. `ConcurrentHashMap` segments the map into buckets; reads are entirely lock-free (using `volatile` reads), and writes lock only the affected bucket. Multiple writers can proceed simultaneously as long as they touch different buckets.

**Q: If you need to read-then-write atomically — e.g., only update a flag if it doesn't already exist — how would you do that?**
Use `ConcurrentHashMap.putIfAbsent()` or `computeIfAbsent()`, which are atomic compare-and-set operations. A plain `get()` followed by a conditional `put()` is not atomic — another thread could insert between the two calls (TOCTOU race). `InMemoryFlagStore` could expose a `putIfAbsent(FlagDefinition)` method backed by `ConcurrentHashMap.putIfAbsent()`.

**Q: If two threads call `store.put("flag-x", def1)` and `store.put("flag-x", def2)` simultaneously, what happens?**
`ConcurrentHashMap.put()` is atomic at the map level — one thread's write will complete before the other's. The final state will be either `def1` or `def2`, never a torn mix of both. Which one wins is non-deterministic (a race), but the result is always a valid, complete `FlagDefinition` object.

**Q: Does `ConcurrentHashMap` guarantee visibility of a newly put flag to all threads immediately?**
Yes. `ConcurrentHashMap` uses `volatile` writes on the bucket entries, which establishes a happens-before relationship. Any thread that reads the map after the write completes is guaranteed to see the new value. There is no need for additional `synchronized` blocks or explicit `volatile` fields.

**Q: What is the ABA problem? Can it affect this codebase?**
The ABA problem occurs in lock-free algorithms when a value changes from A → B → A between a read and a compare-and-swap, making the CAS succeed even though the state changed. It is relevant when CAS-ing on a pointer or primitive. In this codebase, `ConcurrentHashMap` internally handles this correctly using versioned internal nodes. At the application level, since `FlagDefinition` is an immutable record (not a mutable object being modified in place), ABA is not a concern — every update replaces the reference entirely.

**Q: If you added a cache on top of the store using `computeIfAbsent`, and a flag is updated via `put()`, how do you ensure the cache is invalidated atomically with the update?**
You cannot do this atomically across two separate data structures without a lock. The correct approach: wrap both operations in a `synchronized` block, or redesign so the cache and the store are the same structure. A simpler solution is to override `put()` in `CachingFlagStore` to call `cache.remove(flagName)` before or after delegating to the underlying store. There is a small window where a read could fetch the stale cached value — for flag evaluation this is usually acceptable (eventual consistency). If hard consistency is required, use a `ReadWriteLock` to block reads during updates.

**Q: If a background polling thread refreshes flags every 30s, what synchronisation guarantees do you need?**
The polling thread calls `store.put()` (a `ConcurrentHashMap` write). Evaluation threads call `store.get()` (a `ConcurrentHashMap` read). Because `ConcurrentHashMap` already provides visibility guarantees via `volatile`, no additional synchronisation is needed. A plain `volatile` reference to the map itself would not be sufficient if the map were replaced entirely — but since we are mutating entries in the same map, `ConcurrentHashMap`'s internal guarantees are enough.

**Q: How would you implement optimistic locking — reject a `put()` if the flag was modified since you last read it?**
Add a `version` field to `FlagDefinition`. Expose `compareAndPut(FlagDefinition expected, FlagDefinition updated)` on `FlagStore`, backed by `ConcurrentHashMap.replace(key, oldValue, newValue)` which does an atomic compare-and-swap on the value reference. The caller reads the current definition, modifies it into a new record with an incremented version, and calls `compareAndPut`. If another writer changed the flag between the read and write, `replace` returns `false` and the caller retries.

---

### Extensibility

**Q: If I add `DOUBLE` to `FlagType` and forget to handle it in `validateType()`, what happens?**
The Java 21 exhaustive `switch` in `validateType()` will produce a **compile error** — the compiler requires all enum constants to be handled when there is no `default` branch. This is the key advantage of exhaustive `switch` over `if-else` chains: the compiler enforces completeness. You cannot accidentally ship unhandled cases.

**Q: How would you add a `REGEX_MATCH` operator? Walk through every file that changes.**
1. Add `REGEX_MATCH` to `ConditionOperator`.
2. Add a case in `RuleEvaluator.evaluateCondition()`:
```java
case REGEX_MATCH -> contextValue instanceof String s
    && Pattern.matches(conditionValue.toString(), s);
```
3. Add a test case in `RuleEvaluatorTest`. No other files change — the exhaustive switch flags any missed cases.

**Q: How would you support multi-variate flags — `"control"`, `"variant-A"`, `"variant-B"`?**
Already supported. `targetValue` in `Rule` and `value` in `Rollout` are both `Object`. A `STRING` flag can return any string variant. The only change needed is to allow `Rollout` to carry multiple weighted variants (e.g., 33% control, 33% variant-A, 33% variant-B). Model as `List<WeightedVariant>` in `Rollout`, and in `RolloutEvaluator` map the bucket to a cumulative weight range.

**Q: How would you make the bucketing strategy pluggable?**
Extract an interface:
```java
public interface BucketingStrategy {
    int bucket(String flagName, String userId);
}
```
Inject it into `RolloutEvaluator` (or `FlagEvaluator`) via constructor. Ship `Crc32BucketingStrategy` as the default. Callers can provide `MurmurHash3BucketingStrategy` or a `PersistentAssignmentStrategy` that stores bucket assignments in a database.

**Q: How would you add flag dependencies — Flag B only evaluates if Flag A returns `true`?**
Add an optional `dependsOn` field to `FlagDefinition` referencing another flag name. In `FlagEvaluator`, before evaluating a flag, resolve its dependency recursively. Guard against circular dependencies by tracking a `Set<String>` of flags currently being evaluated in the call stack — throw `CircularFlagDependencyException` if a flag appears twice. Limit recursion depth to prevent stack overflow.

**Q: How would you add per-user overrides — a specific user always gets a specific value regardless of rules or rollout?**
Add an `overrides` map to `FlagDefinition`:
```java
Map<String, Object> overrides  // userId → value
```
In `FlagEvaluator`, check `overrides.get(ctx.userId())` first, before rule evaluation. Return immediately with reason `OVERRIDE`. This is the highest-priority path and is the standard way flag SDKs handle QA/testing users.

**Q: How would you design a plugin system for custom `ConditionOperator` implementations?**
Replace the `enum ConditionOperator` with an interface:
```java
public interface ConditionOperator {
    String name();
    boolean evaluate(Object contextValue, Object conditionValue);
}
```
Ship built-in operators as constants. `RuleEvaluator.evaluateCondition()` calls `condition.operator().evaluate(contextValue, conditionValue)` — no switch needed. Third parties implement the interface and register it. The trade-off: you lose exhaustive compile-time checking of operators.

**Q: How would you version flag definitions to support rollback?**
Add `version` (long) and a `previousVersions` list (or store versions in a separate `FlagVersionStore`). `FlagStore.put()` increments version and archives the old definition. Add `rollback(String flagName)` to `FeatureFlagClient` that restores the previous version atomically. An event log (append-only list of `FlagChangeEvent`) is the more scalable alternative to storing full previous definitions.

**Q: What's missing from `FlagStore` to support bulk load and delta sync?**
```java
public interface FlagStore {
    void put(FlagDefinition flag);
    Optional<FlagDefinition> get(String flagName);
    void remove(String flagName);

    // Missing for bulk load:
    void putAll(Collection<FlagDefinition> flags);

    // Missing for delta sync:
    Map<String, Long> getVersions();         // flagName → version
    Collection<FlagDefinition> getAll();
}
```
Delta sync: the client sends its current `getVersions()` map to the server; the server returns only changed flags. This avoids re-fetching all flag definitions on every poll cycle.

---

### Design Decisions

**Q: Why are model classes records and not regular classes? What would break if `Rule` were mutable?**
If `Rule` were mutable, a caller could do `rule.conditions().clear()` after registering a flag, silently breaking all future evaluations of that flag. With records and `List.copyOf()` in the constructor, the caller's list and the stored list are independent. Immutability also means no thread needs to lock a `Rule` while reading it — reads are always safe.

**Q: `FlagEvaluator` hardcodes `RuleEvaluator` and `RolloutEvaluator`. Is that a problem?**
For the current scope, no — they have no dependencies and no configuration. If you want to make the bucketing strategy pluggable or add metrics to rule evaluation, inject them:
```java
public FlagEvaluator(FlagStore store, RuleEvaluator ruleEvaluator, RolloutEvaluator rolloutEvaluator)
```
Keep the no-arg convenience constructor that wires the defaults. This follows the "constructor injection with sensible defaults" pattern.

**Q: Why does `EvaluationResult` carry a `reason`? Why not just return the value?**
The value alone tells you *what* the user gets; the reason tells you *why*. Without the reason, debugging "why is this user getting `false`?" requires guessing whether it was the default, a non-matching rule, or exclusion from rollout. With `RULE_MATCH | ROLLOUT | DEFAULT`, you can log the reason on every evaluation and build dashboards showing how many evaluations are hitting each path per flag.

**Q: `FlagDefinition` stores `defaultValue` and `targetValue` as raw `Object`. What are the risks?**
A flag of type `BOOLEAN` could accidentally store `"hello"` as its `defaultValue`. At evaluation time, `cast(flag.defaultValue(), Boolean.class)` throws `ClassCastException`. The fix is type validation in the `FlagDefinition` compact constructor (checking `defaultValue instanceof Boolean` for `BOOLEAN` flags). For full type safety without raw `Object`, use a generic `FlagDefinition<T>` — but that complicates storing heterogeneous flags in a single `Map<String, FlagDefinition<?>>`.

**Q: Why is `rules` a `List` (ordered) rather than a `Set`?**
Order is the contract — first-match-wins depends on a deterministic, caller-controlled evaluation sequence. A `Set` has no defined iteration order. The caller explicitly puts the most specific rule first and the most general rule last. Using a `List` makes this contract visible and enforced.

**Q: `FeatureFlagClient` is a class, not an interface. What problem does that create for callers writing tests?**
Callers cannot mock it with standard mocking frameworks without either subclassing (fragile) or using byte-buddy-based mocks (heavier). Fix: extract `FeatureFlagOperations` interface with `getBooleanFlag`, `getStringFlag`, `getIntegerFlag`, `evaluate`. `FeatureFlagClient` implements it. Callers depend on the interface — tests inject a fake or mock.

**Q: How does this design compare to the Strategy pattern? Is `RuleEvaluator` a strategy?**
Yes. `RuleEvaluator` and `RolloutEvaluator` are strategies — they encapsulate the *algorithm* for evaluating a rule and computing a bucket, respectively. `FlagEvaluator` is the context that selects and delegates to them. Formalizing this (injecting them via constructor) makes it explicit and allows swapping algorithms — e.g., replacing `RuleEvaluator` with a `CachingRuleEvaluator` that memoizes results for the same context.

---

### Rollout & Bucketing Deep Dive

**Q: Why is the bucket key `flagName + "|" + userId` and not just `userId`?**
If the key were just `userId`, the bucket for user `alice` would be the same for every flag. A user in the 20% rollout for `flag-A` would automatically be in the 20% rollout for every other flag — rollouts would be perfectly correlated. Including the flag name in the key ensures each flag produces an independent, uncorrelated bucket assignment for the same user.

**Q: What is sticky bucketing? Why does it matter for UX?**
Sticky bucketing means the same user always gets the same rollout result — even if the application restarts, the flag is re-evaluated, or the request is handled by a different server. Without stickiness, a user could see a new feature appear and disappear randomly across page loads. CRC32's determinism (no state required) provides stickiness for free.

**Q: What happens to a user's bucket if the flag is deleted and re-registered with the same name?**
The bucket is recomputed identically — `CRC32(flagName|userId) % 100` depends only on the flag name and user ID, not on registration time or flag version. The user gets the same bucket as before. This is a feature for stability but a risk if you intentionally want to re-shuffle rollout assignments — you'd need to change the flag name or add a version seed to the key.

**Q: Is the distribution of `CRC32 % 100` perfectly uniform?**
No, but it is close. CRC32 produces a 32-bit value (0 to 4,294,967,295). Since 4,294,967,295 is not divisible by 100, values 0–95 map to 43,000 possible CRC32 outputs and values 96–99 map to 42,949 — a ~0.1% bias. For rollout bucketing this is negligible. MurmurHash3 has better avalanche properties and is commonly preferred in production flag systems (e.g., it is what LaunchDarkly uses).

**Q: If rollout changes from 50% to 30%, some users who were in the rollout get excluded. Is that acceptable?**
It depends on the flag's purpose. For a UI change, flip-flopping is a bad user experience. For a backend experiment, it may invalidate statistical results. The `% 100` scheme is monotonically *increasing* (adding users as percentage grows) but not monotonically *decreasing* (it removes users when percentage shrinks). If monotonic decrease is required, use a persistent assignment store — once a user is assigned, the assignment is stored and never changes regardless of percentage adjustments.

**Q: How would you bucket on a company rather than a user?**
Pass the company ID as `userId` in `EvaluationContext`. The bucket key becomes `flagName + "|" + companyId`. All users within the same company get the same bucket — ensuring consistent experience within a company. This is a common pattern called "organization-level rollout."

**Q: What property does MurmurHash3 have that makes it better than CRC32 for bucketing?**
The **avalanche effect** — a 1-bit change in the input causes approximately 50% of the output bits to change. This produces a highly uniform distribution of hash values even for inputs that are similar (like `"user-1"`, `"user-2"`, `"user-3"`). CRC32 was designed for error detection, not distribution uniformity, so similar inputs can produce clustered outputs. For large user populations the difference is negligible, but MurmurHash3 is the correct choice for production systems.

**Q: How would you implement guaranteed exactly-N%-of-users rollout?**
Assign each user a permanent random bucket (0–99) on first encounter, stored in a database. On evaluation, look up the stored bucket. This guarantees: (1) each user has an exactly uniform 1-in-100 chance of being in any given bucket, (2) assignments are permanent and never re-computed, (3) rollout is strictly monotonic in both directions. The trade-off is a database lookup per evaluation for new users.

---

### Testing

**Q: How do you test that rollout is sticky without making the test non-deterministic?**
CRC32 is deterministic — given the same `flagName` and `userId`, it always produces the same bucket. The test fixes both inputs and calls `evaluate()` 20 times, asserting the result never changes. There is no randomness involved; the test is fully deterministic and will never flake.

**Q: What N gives 99% confidence the distribution test won't flake for a 50% rollout with ±10% tolerance?**
Using the normal approximation for a binomial distribution: standard deviation = `sqrt(N * p * (1-p))`. For p=0.5, N=1000: σ ≈ 15.8, so a ±100 count window (±10%) is ~6.3 standard deviations wide — effectively zero flake probability. The test uses N=1000 with a 40–60% window, which is very safe.

**Q: How would you test that `FlagEvaluator` is thread-safe under concurrent reads and writes?**
```java
CountDownLatch start = new CountDownLatch(1);
ExecutorService pool = Executors.newFixedThreadPool(20);

// 18 reader threads
for (int i = 0; i < 18; i++) {
    pool.submit(() -> {
        start.await();
        for (int j = 0; j < 1000; j++) evaluator.evaluate("flag", ctx, Boolean.class);
    });
}
// 2 writer threads
for (int i = 0; i < 2; i++) {
    pool.submit(() -> {
        start.await();
        for (int j = 0; j < 100; j++) store.put(newFlagDefinition(j));
    });
}
start.countDown(); // release all threads simultaneously
pool.shutdown();
pool.awaitTermination(10, TimeUnit.SECONDS);
// Assert no exceptions were thrown
```
Run with `-Djava.util.concurrent.ForkJoinPool.common.parallelism=8` to maximize thread interleaving.

**Q: How would you write a mutation test to verify AND vs OR logic?**
Use PIT (Pitest) mutation testing — it automatically mutates `allMatch` → `anyMatch` and vice versa and checks if any existing test catches the change. If no test fails when `allMatch` is mutated to `anyMatch`, your test suite has a gap. The current `RuleEvaluatorTest` has separate tests for AND-all-match, AND-partial-match, OR-any-match, and OR-none-match — PIT would confirm these mutations are caught.

**Q: How would you property-test the bucketing algorithm for stickiness and uniformity?**
Using jqwik:
```java
@Property
void stickiness(@ForAll String flagName, @ForAll String userId, @ForAll @IntRange(min=0, max=100) int pct) {
    Rollout rollout = new Rollout(pct, true);
    Optional<Object> first = evaluator.evaluate(flagName, userId, rollout);
    assertThat(evaluator.evaluate(flagName, userId, rollout)).isEqualTo(first);
}

@Property
void uniformity(@ForAll @Size(1000) List<String> userIds) {
    long inRollout = userIds.stream()
        .filter(uid -> evaluator.evaluate("flag", uid, new Rollout(50, true)).isPresent())
        .count();
    assertThat(inRollout).isBetween(400L, 600L);
}
```
jqwik generates hundreds of random inputs and verifies the invariants hold for all of them — far more coverage than hand-written examples.
