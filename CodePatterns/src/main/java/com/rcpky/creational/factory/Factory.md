# 📔 Software Design Notes: The Factory Design Pattern

## 1. Core Concept & Structure
The **Factory Design Pattern** is a creational pattern that abstracts and centralizes the object instantiation process, separating the client code from concrete implementations.

*   **Product:** The common interface or abstract class defining what the factory builds.
*   **Concrete Product:** The specific implementation classes.
*   **Factory/Creator:** The class containing the logic to choose, instantiate, and return the `Product`.

### Pros & Cons of the Factory Pattern
#### 👍 Pros
*   **Loose Coupling:** Separates object usage from object creation logic.
*   **Open/Closed Principle (OCP):** New product types can be introduced without breaking existing client code.
*   **Single Responsibility Principle (SRP):** Centralizes object creation code in one place rather than scattering it across the codebase.
*   **Encapsulation of Complexity:** Hides complex initialization routines (e.g., setting up dependencies, loading configurations) from the client.

#### 👎 Cons
*   **Over-engineering Risk:** Introduces unnecessary layers of abstraction for simple, unchanging classes.
*   **Code Bloat:** Requires creating new interfaces, factories, and subclasses, which increases the total number of files in a project.

---

## 2. Thread Safety Considerations
*   **Stateless Factories:** If the factory only contains execution logic and holds no shared mutable state, it is **inherently thread-safe**. Multiple threads can invoke it concurrently.
*   **Stateful Factories:** If the factory tracks metrics, maintains configuration state, or caches objects, access must be synchronized using mechanisms like `ConcurrentHashMap` or explicit locks.
*   **Singleton Factories:** When implementing a factory itself as a singleton, use thread-safe initialization strategies like eager initialization, enum singletons, or double-checked locking.

---

## 3. Implementation Strategies

### Strategy A: Simple Factory (Switch-Case)
*   **Pros:** Straightforward and easy to implement.
*   **Cons:** Violates the Open/Closed Principle; you must manually modify the code whenever a new product is added. Worst-case lookup time is linear \(O(N)\).

```java
public class SimpleFactory {
    public Notification createNotification(String type) {
        if (type == null) return null;
        switch (type.toUpperCase()) {
            case "EMAIL": return new EmailNotification();
            case "SMS":   return new SMSNotification();
            default: throw new IllegalArgumentException("Unknown type");
        }
    }
}
```

### Strategy B: High-Performance Registry Factory (Dynamic Mapping)
*   **Pros:** Adheres to OCP. Allows dynamic runtime registration of new products without modifying the factory. Achieves **\(O(1)\) constant lookup time**.
*   **Cons:** More abstract; requires understanding of functional interfaces (e.g., Java `Supplier`).

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class AdvancedNotificationFactory {
    private static final Map<String, Supplier<Notification>> registry = new ConcurrentHashMap<>();

    public static void registerProduct(String type, Supplier<Notification> supplier) {
        registry.put(type.toUpperCase(), supplier);
    }

    static {
        registerProduct("EMAIL", EmailNotification::new);
        registerProduct("SMS", SMSNotification::new);
    }

    public static Notification createNotification(String type) {
        Supplier<Notification> supplier = registry.get(type.toUpperCase());
        if (supplier == null) throw new IllegalArgumentException("Unknown type");
        return supplier.get();
    }
}
```

---

## 4. Performance & Garbage Collection (GC) Pressure

### The Problem
Calling `new Product()` inside a factory on every single request generates high **GC Pressure** in high-throughput applications (millions of requests per second). This leads to memory thrashing and frequent "Stop-the-World" GC pauses.

### The 3 Solutions

#### 🛠️ Solution 1: Pre-Instantiated Singleton Cache (For Stateless Objects)
If objects only execute code logic and contain **no user-specific state**, reuse the exact same instances to achieve **zero GC pressure**.

```java
public class CacheNotificationFactory {
    private static final Map<String, Notification> cache = new ConcurrentHashMap<>();

    static {
        cache.put("EMAIL", new EmailNotification());
        cache.put("SMS", new SMSNotification());
    }

    public static Notification getNotification(String type) {
        Notification instance = cache.get(type.toUpperCase());
        if (instance == null) throw new IllegalArgumentException("Unknown type");
        return instance; 
    }
}
```

#### 🛠️ Solution 2: Object Pooling / Flyweight (For Heavy, Stateful Objects)
If objects are mutable or resource-heavy (like DB connections), borrow them from a structured **Object Pool** and return them when finished instead of discarding them.

#### 🛠️ Solution 3: Virtual Threads & ZGC (Modern JVM Optimization)
In modern runtime environments (e.g., Java 21+), pair lightweight Virtual Threads with a low-latency garbage collector like **ZGC**. Small, short-lived objects are either allocated directly to the CPU stack via escape analysis or collected in milliseconds with negligible performance impact.
