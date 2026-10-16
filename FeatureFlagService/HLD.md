# HLD — Feature Flag Evaluation Service

Design a feature flag evaluation service that allows engineers to enable or disable named features for specific users at runtime without redeploying code.

---

## Requirements

### Functional
- CRUD on flags — each flag has a name, a Boolean on/off state, and a user allowlist targeting rule
- Single and batched flag evaluation by `userId`
- Runtime flag updates — no code redeploy

### Non-Functional
- Up to **1,000 active flags**
- **10,000 flag evaluation requests/sec** from backend services
- **P95 latency < 10ms** for both single and batched evaluations
- High availability — evaluation must work even if the management plane is down

### Assumptions
- Allowlist = set of user IDs; evaluation = "is userId in allowlist AND flag is enabled?"
- Eventual consistency on flag updates is acceptable (sub-second propagation is sufficient)
- Callers are internal backend services (same datacenter)

---

## Capacity Estimation

| Metric | Value |
|---|---|
| Active flags | 1,000 |
| Avg allowlist size | ~10,000 user IDs × 36 bytes (UUID) ≈ 360 KB/flag |
| **Total flag data** | **~360 MB worst case — fits in a single node's heap** |
| Requests/sec | 10,000 |
| Avg request payload | ~100 bytes |
| **Throughput** | **~1 MB/s (trivial)** |

**Key insight**: all 1,000 flags fit in memory on a single node. This is purely a *cache design + change propagation* problem, not a storage problem.

---

## High-Level Architecture

```
┌──────────────┐   REST    ┌───────────────────────────┐
│ Admin Portal │ ────────► │   Flag Management Service  │
│ / Engineers  │           │   (CRUD, versioning,       │
└──────────────┘           │    audit log)              │
                           └──────────┬────────────────┘
                                      │  Write (sync)
                                      ▼
                           ┌───────────────────────────┐
                           │   PostgreSQL               │
                           │   (Source of truth)        │
                           └──────────┬────────────────┘
                                      │  Publish on write
                                      ▼
                           ┌───────────────────────────┐
                           │   Redis Pub/Sub            │
                           │   (flag-updates channel)   │
                           └──────────┬────────────────┘
                                      │  Subscribe (persistent conn)
                          ┌───────────┼───────────────┐
                          ▼           ▼               ▼
               ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
               │  Eval Node 1 │  │  Eval Node 2 │  │  Eval Node N │
               │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
               │ │In-Memory │ │  │ │In-Memory │ │  │ │In-Memory │ │
               │ │Flag Cache│ │  │ │Flag Cache│ │  │ │Flag Cache│ │
               │ │(HashMap) │ │  │ │(HashMap) │ │  │ │(HashMap) │ │
               │ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │
               └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
                      └─────────────────┼─────────────────┘
                                        │  gRPC / HTTP/2
                               ┌────────┴────────┐
                               │  Load Balancer  │
                               └────────┬────────┘
                                        │
                          ┌─────────────┴─────────────┐
                          │     Backend Services       │
                          │  (SDK / gRPC client)       │
                          └───────────────────────────┘
```

---

## Component Deep-Dives

### Flag Management Service (Write Path)

**Responsibilities**: CRUD on flags, input validation, versioning, audit trail.

**Write flow:**
```
Engineer  PUT /flags/{name}
    → validate input
    → UPDATE flags SET enabled=?, allowlist=?, version=version+1 WHERE name=?
    → PUBLISH "flag-updated" event to Redis { name, enabled, allowlist, version }
    → return 200 OK
```

- Uses **optimistic locking** via a `version` column — prevents lost updates when two engineers edit simultaneously
- Every write appended to an `audit_log` table (who changed what, when)
- Stateless — multiple replicas can run behind a load balancer

---

### Flag Evaluation Service (Read Path — critical path)

**This is where P95 < 10ms lives. All evaluation is in-process.**

**On startup:**
```
1. Load all flags from PostgreSQL into ConcurrentHashMap<String, FlagState>
2. Subscribe to Redis "flag-updates" channel
3. Start accepting traffic
```

**On flag update received from Redis:**
```
1. Deserialize the event
2. cache.put(flagName, newFlagState)   ← atomic reference swap
3. Done — next evaluation sees the new state immediately
```

**On evaluation request:**
```java
FlagState flag = cache.get(flagName);        // ~100 ns
if (flag == null)      return NOT_FOUND;
if (!flag.enabled())   return false;
return flag.allowlist().contains(userId);    // HashSet O(1) ~100 ns
```

Total in-process time: **< 1 µs**. Network RTT + serialization brings real P95 to **2–5 ms**.

---

### In-Memory Cache Design

```
ConcurrentHashMap<String, FlagState>
    flagName → FlagState {
        boolean        enabled
        HashSet<String> allowlist
        long           version
    }
```

- `ConcurrentHashMap` — lock-free reads; no reader blocks any writer
- `FlagState` is an **immutable value object** — the entire object is replaced atomically on update; no reader ever sees a partial/torn state
- `HashSet.contains()` is O(1) average — 10,000 user IDs checked in ~100 ns

**Large allowlist optimization**: If an allowlist grows to millions of user IDs, front the `HashSet` with a **Bloom filter**. A Bloom hit (user *might* be in list) falls through to the `HashSet`; a Bloom miss (user *definitely* not in list) short-circuits in nanoseconds with zero false negatives.

---

### Change Propagation — Redis Pub/Sub

```
Flag Management         Redis              Eval Node
       │                  │                    │
  PUT /flag               │                    │
       │── PUBLISH ──────►│                    │
       │                  │──── PUSH ─────────►│
       │                  │                    │ cache.put(flag)  < 1ms
```

- **Propagation latency**: ~10–50 ms (same datacenter)
- **Fallback**: if a node misses a message (network blip), it reconciles by polling PostgreSQL every 30 s

**Why Redis Pub/Sub over Kafka?**
- Flag updates are rare (< 100/day) — Kafka's durability and consumer-group mechanics are overkill
- Redis Pub/Sub delivers in ~1 ms; the 30 s polling fallback closes the reliability gap
- Choose Kafka if you need full audit-stream replay or cross-region fan-out

---

## API Design

### Evaluation API (gRPC preferred; HTTP/2 REST fallback)

**Single evaluation**
```
GET /v1/flags/{flagName}/evaluate?userId=user-123

200 OK
{
  "flagName": "new-checkout",
  "userId":   "user-123",
  "enabled":  true,
  "reason":   "ALLOWLIST"    // ALLOWLIST | FLAG_OFF | FLAG_NOT_FOUND
}
```

**Batch evaluation** — evaluates N flags in one round-trip (key for P95)
```
POST /v1/flags/evaluate
{
  "userId": "user-123",
  "flags":  ["new-checkout", "dark-mode", "beta-search"]
}

200 OK
{
  "results": {
    "new-checkout": { "enabled": true,  "reason": "ALLOWLIST" },
    "dark-mode":    { "enabled": false, "reason": "FLAG_OFF"  },
    "beta-search":  { "enabled": true,  "reason": "ALLOWLIST" }
  }
}
```

### Management API

```
POST   /v1/flags                  ← create flag
GET    /v1/flags                  ← list all flags (admin UI)
GET    /v1/flags/{name}           ← get one flag
PUT    /v1/flags/{name}           ← full update (state + allowlist)
PATCH  /v1/flags/{name}/state     ← toggle on/off only (kill-switch)
DELETE /v1/flags/{name}
GET    /v1/flags/{name}/audit     ← change history
```

---

## Data Model

```sql
CREATE TABLE flags (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) UNIQUE NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    allowlist   TEXT[]       NOT NULL DEFAULT '{}',
    version     BIGINT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_name   VARCHAR(255) NOT NULL,
    changed_by  VARCHAR(255) NOT NULL,
    old_state   JSONB,
    new_state   JSONB,
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_flags_name     ON flags(name);
CREATE INDEX idx_audit_flag     ON audit_log(flag_name, changed_at DESC);
```

`TEXT[]` for allowlist — PostgreSQL can index and query arrays natively. For very large allowlists (millions of entries), normalize to a separate `flag_allowlist(flag_id UUID, user_id TEXT)` table with a composite index.

---

## Latency Budget — P95 < 10 ms

```
Client SDK (same DC)
    │
    │  1.0 ms  ← network RTT (HTTP/2 keepalive connection, same DC)
    ▼
Load Balancer
    │
    │  0.5 ms  ← LB routing overhead
    ▼
Eval Node
    │
    │  < 1 µs  ← ConcurrentHashMap.get() + HashSet.contains()
    │  0.5 ms  ← serialization (JSON / protobuf)
    ▼
Response
    │
    │  1.0 ms  ← network RTT return
    ▼
Client

  P50 ≈ 2 ms  |  P95 ≈ 4–6 ms  |  P99 ≈ 8 ms  ✓
```

**Batch evaluation** processes N flags over the *same* connection in the same ~3 ms — no additional I/O per flag added.

---

## Failure Modes & Mitigations

| Failure | Impact | Mitigation |
|---|---|---|
| **Management Service down** | Engineers can't update flags | Eval service continues serving from warm in-memory cache; no evaluation downtime |
| **PostgreSQL down** | Can't persist new flag changes | Eval cache serves all traffic; mgmt service rejects writes with 503 |
| **Redis Pub/Sub down** | Flag updates stop propagating to eval nodes | 30 s polling fallback; max staleness = 30 s |
| **Eval node cold start** | Empty cache on boot | Load all flags from PG on startup (~360 MB at ~100 MB/s = < 4 s); node excluded from LB until warm |
| **Flag not found** | Unknown flag name in request | Return `enabled: false`, reason `FLAG_NOT_FOUND` — safe default |
| **Allowlist too large** | `HashSet` memory pressure | Cap at 100K entries per flag; use Bloom filter for larger sets |

---

## Scaling

**Eval service is horizontally stateless** — adding pods linearly increases throughput:

```
3 eval nodes × ~5,000 req/sec each = 15,000 req/sec  (50% headroom)
```

Each node maintains its own in-memory cache via the same Redis subscription — no coordination between nodes is required. The load balancer routes any request to any node.

**Management service** bottleneck is the PostgreSQL write path, but flag updates are infrequent (< 100/day), so a single primary with one read replica is sufficient.

---

## Trade-offs Summary

| Decision | Choice | Alternative | Why |
|---|---|---|---|
| **Consistency model** | Eventual (ms → 30 s) | Strong (sync broadcast) | Acceptable for flags; strong consistency adds latency and coordination complexity |
| **Update propagation** | Redis Pub/Sub + polling fallback | Kafka | Simpler; polling closes the reliability gap; Kafka is overkill for infrequent writes |
| **Allowlist in eval** | `HashSet` in-process | Redis `SISMEMBER` per request | In-memory avoids a network hop entirely; HashSet O(1) vs Redis ~1 ms |
| **Eval protocol** | gRPC / HTTP/2 | REST HTTP/1.1 | HTTP/2 multiplexing and connection reuse cut latency for batched calls |
| **Allowlist storage in DB** | PostgreSQL `TEXT[]` | Separate normalized table | Simple for ≤ 100K entries; normalize when allowlists grow to millions |
| **Flag state object** | Immutable value object | Mutable + synchronized | Immutability gives lock-free concurrent reads; no torn-state risk |
