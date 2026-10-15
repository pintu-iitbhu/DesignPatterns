# InMemoryCache

A high-performance, thread-safe in-memory caching library with configurable eviction policies and automatic entry expiration.

## ✅ Expectations from Interviewer

You're expected to:

- Design a generic, reusable, in-memory cache library
- Support pluggable eviction policies like LRU, TTL
- Ensure thread safety
- Allow configuration for max size, eviction strategy, TTL per entry
- Possibly expose APIs like put(K key, V value), get(K key), remove(K key), size()
- Write clean code with proper abstractions and testable structure

## Overview

This InMemoryCache implementation provides a generic, thread-safe caching solution with the following features:

- Generic key-value pair storage
- Thread-safety using ReentrantLock
- Configurable maximum capacity
- Pluggable eviction policies (LRU implementation provided)
- Time-based entry expiration
- O(1) complexity for common operations (get, put, remove)

## Architecture

The system follows a modular design with clean separation of concerns:

### Core Components

1. **Cache Interface (`Cache<K,V>`)**: Defines the contract for all cache implementations
   - `get(K key)`: Retrieve a value by key
   - `put(K key, V value)`: Store a value with the given key
   - `remove(K key)`: Remove an entry by key
   - `size()`: Get the current cache size

2. **InMemoryCache Implementation**: Thread-safe cache implementation using HashMap for storage
   - Implements automatic eviction when capacity is reached
   - Supports entry expiration
   - Thread-safe operation using ReentrantLock

3. **Eviction Policy Interface (`EvictionPolicy<K>`)**: Defines contract for cache eviction strategies
   - `evictKey()`: Determine which key to evict next
   - `keyAccessed(K key)`: Update access metadata for a key

4. **LRU Eviction Policy**: Least Recently Used implementation
   - Uses a doubly linked list for O(1) operations
   - Maintains access order for efficient eviction decisions

5. **Supporting Classes**:
   - `CacheEntry<V>`: Wrapper for values with expiration metadata
   - `DoublyLinkedList<K>`: Custom doubly linked list for LRU implementation
   - `Node<K>`: Node in the doubly linked list

## Class Diagram

```
┌───────────────┐     uses     ┌──────────────────┐
│  Cache<K,V>   │◄────────────┤    Client Code    │
└───────┬───────┘              └──────────────────┘
        │
        │ implements
        ▼
┌───────────────────┐    uses    ┌────────────────┐
│ InMemoryCache<K,V>│◄──────────┤EvictionPolicy<K>│
└───────────────────┘            └────────┬───────┘
        │                                 │
        │ contains                        │ implements
        ▼                                 ▼
┌───────────────┐               ┌─────────────────┐
│CacheEntry<V>   │              │LRUEvictionPolicy│
└───────────────┘               └────────┬────────┘
                                         │
                                         │ uses
                                         ▼
                                ┌─────────────────┐
                                │DoublyLinkedList │
                                └────────┬────────┘
                                         │
                                         │ contains
                                         ▼
                                ┌─────────────────┐
                                │     Node<K>     │
                                └─────────────────┘
```

## Usage Examples

### Basic Usage

```java
// Create cache with LRU eviction and capacity of 100
Cache<String, User> userCache = new InMemoryCache<>(new LRUEvictionPolicy<>(), 100);

// Store a value
userCache.put("user:1", new User("Alice", "alice@example.com"));

// Retrieve a value
User user = userCache.get("user:1");

// Remove a value
userCache.remove("user:1");
```

### Custom Eviction Policy

You can implement your own eviction policy by implementing the `EvictionPolicy<K>` interface:

```java
public class FIFOEvictionPolicy<K> implements EvictionPolicy<K> {
    private final Queue<K> queue = new LinkedList<>();
    private final Set<K> keySet = new HashSet<>();
    
    @Override
    public K evictKey() {
        if (queue.isEmpty()) return null;
        K key = queue.poll();
        keySet.remove(key);
        return key;
    }
    
    @Override
    public void keyAccessed(K key) {
        if (!keySet.contains(key)) {
            queue.add(key);
            keySet.add(key);
        }
    }
}
```

## Thread Safety

The InMemoryCache implementation is thread-safe using Java's ReentrantLock. All cache operations (get, put, remove, size) are synchronized to ensure consistent behavior in a concurrent environment.

## Testing

The caching system includes a comprehensive test suite covering:

1. Basic operations (put, get, remove, size)
2. Eviction policy behavior
3. Entry expiration
4. Thread safety with concurrent operations
5. Edge cases (null values, policy interaction)

To run the tests:

```bash
mvn test
```

## Performance Considerations

- Get/Put operations have O(1) complexity
- Thread synchronization may impact performance under high concurrency
- The LRU implementation maintains O(1) complexity for all operations
- Memory usage is proportional to the configured capacity
- Expired entries are lazily removed on access

## Future Enhancements

- Add support for write-through/write-behind strategies
- Implement additional eviction policies (LFU, FIFO, etc.)
- Add statistics collection for cache hits/misses
- Support for distributed caching
- Add callback mechanisms for eviction events


# 📚 In-Memory Cache Design – Interview Q&A

This document covers common **cross-questions** asked in interviews for the low-level design of a **Generic In-Memory Cache Library**, along with strong, SDE-2-level answers.

---

## 🚀 Summary of the Problem

Design a **generic**, **thread-safe**, **pluggable**, and **extensible** in-memory cache that supports:
- Generic `K, V` types
- Eviction policies (e.g., **LRU**, **TTL**)
- Optional expiration per entry
- Thread-safe concurrent access
- Clean API and testability

---

## 🧠 Cross-Question & Answer List

### 🔐 Concurrency & Thread-Safety

#### Q1: How does your cache handle concurrent access?
**A:**  
I use a `ReentrantLock` to synchronize critical sections involving the internal `Map` and eviction policy, ensuring thread safety for reads/writes. For better performance under read-heavy loads, a `ReadWriteLock` or sharded segments can be introduced.

---

#### Q2: Why not use `ConcurrentHashMap`?
**A:**  
`ConcurrentHashMap` ensures atomic access to the map, but doesn't coordinate with eviction logic (like LRU’s doubly linked list). We need atomicity across both cache and policy — thus explicit locking ensures consistency.

---

#### Q3: How can you improve concurrency further?
**A:**
- Use **segmented locking** or shard the cache
- Switch to `StampedLock` or per-key locking
- Use `ConcurrentHashMap` + `ConcurrentLinkedDeque` and CAS-based logic

---

### ♻️ Eviction Policies

#### Q4: Why use HashMap + DLL for LRU?
**A:**  
It enables **O(1)** time complexity for `get`, `put`, and `evict` operations:
- HashMap: key lookup
- Doubly Linked List: maintain order of access

---

#### Q5: Can you support LFU/FIFO or other strategies?
**A:**  
Yes. The `EvictionPolicy<K>` interface allows plugging in custom policies (LFU, FIFO) without changing core cache logic.

---

### ⏳ TTL (Time-To-Live)

#### Q6: How is TTL handled?
**A:**  
Each entry has an `expiryTimeMillis`. During `get()`, we check if it’s expired and evict it lazily. TTL is validated inline to avoid complexity of per-entry timers.

---

#### Q7: What are pros/cons of background TTL cleanup?
**Pros:**
- Frees memory earlier
- Keeps cache size cleaner

**Cons:**
- Overhead of extra thread
- Needs proper locking/synchronization
- Tuning cleanup interval is non-trivial

---

### ⚖️ Trade-Offs

#### Q8: Why not use Guava or Caffeine?
**A:**  
While production systems should use mature libraries, the custom implementation showcases understanding of:
- OOP design
- Eviction policies
- Thread safety
- System extensibility

---

#### Q9: What are the trade-offs of this design?
**A:**
- Higher memory usage (due to metadata)
- Locking introduces contention under high concurrency
- Lazy TTL eviction may hold expired entries longer unless cleaned

---

### 🧪 Testing & Extensibility

#### Q10: How would you test this cache?
**A:**
- Unit tests for `put`, `get`, `eviction`, `remove`
- Expiry tests using mockable time
- Multithreaded stress tests using `ExecutorService`
- Metric validation (hit/miss)

---

#### Q11: Can it support async refresh?
**A:**  
Yes. Add a `CacheLoader<K, V>` interface. If value is expired or missing, reload asynchronously and serve stale data in the meantime (to avoid stampede).

---

### 📊 Metrics & Monitoring

#### Q12: How do you add cache hit/miss metrics?
**A:**  
Use `AtomicLong` counters for:
- Hits (valid entry)
- Misses (absent or expired)
  Expose these via an endpoint or metrics interface for observability.

---

### 🧠 Advanced / Distributed Support

#### Q13: Can this be extended to a distributed cache?
**A:**  
Yes. Add a storage interface and implement a Redis-backed or Hazelcast-backed cache. Also introduce:
- Distributed invalidation
- Partitioning
- Serialization support

---

#### Q14: What is a cache stampede? How to avoid it?
**A:**  
A stampede happens when many threads try to load the same missing/expired key. Prevent via:
- Per-key locking
- Double-checked locking
- Serve stale while loading new value

---

#### Q15: How would you support write-through or write-behind caching?
**A:**  
Introduce a `CacheWriter<K, V>` interface:
- **Write-through**: write to cache + DB synchronously
- **Write-behind**: queue writes and persist asynchronously using a background thread

---

## ✅ Summary

This cache design focuses on:
- Clean separation of concerns
- Extensibility via interfaces
- Thread safety and eviction accuracy
- Performance via `O(1)` operations for LRU

It’s designed to serve as a reusable utility across microservices with consistent behavior and observability.

---

## 📁 Bonus Suggestions

- Implement this in a repo with:
   - Interfaces
   - Core logic
   - Test cases
   - `README.md` as documentation
- Optionally use Spring Boot to expose cache metrics
- Add Prometheus integration to track cache stats

---
