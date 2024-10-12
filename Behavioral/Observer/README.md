# Oberver Design Pattern
The **Observer Design Pattern** is a behavioral design pattern that lets you define a subscription mechanism to notify multiple objects about any events that happen to the object they’re observing.

# Key Concepts:
1. **Observer Interface**: Declares an update method that the subject calls when its state changes.
2. **Concrete Observer**: Implements the observer interface, providing specific implementations for the update method.
3. **Subject Interface**: Declares an attach method that the observer calls to register itself with the subject.
4. **Concrete Subject**: Implements the subject interface, providing specific implementations for the attach method.


# Benefits:
- **Separation of Concerns**: The pattern separates the operations (algorithms) from the object structure.
- **Open/Closed Principle** : You can add new operations by introducing new observers without changing the existing subject classes.
- **Extensibility**: The pattern is especially useful when you need to perform different kinds of operations on a set of objects that belong to different classes.
- **Multiple Observers**: The pattern allows you to register multiple observers with the subject.

# Advantages:
- **Loose Coupling**: Subjects and observers are loosely coupled; subjects don't need to know the concrete classes of observers.
- **Dynamic Relationships**: Observers can be added or removed at runtime, providing flexibility.
- **Reusability**: The pattern promotes reusability of subjects and observers across different contexts.

# Disadvantages:
- **Memory Leaks**: If observers are not properly detached, it can lead to memory leaks.
- **Unexpected Updates**: Observers receive all updates, which might not always be necessary.
- **Complexity**: In systems with many observers, managing dependencies can become complex.

# Example: Observer Pattern in Java

# 1. Observer Interface
The interface defines a method that all observers must implement.
```java
// Observer Interface
public interface NotificationAlertObserver {
    void Update();
}
```       

# 2. Concrete Observer  
The concrete observer class implements the interface and provides its own implementation for the update method.
```java
// Concrete Observer
public class EmailAlertObserverImpl implements NotificationAlertObserver {

    @Override
    public void Update() {
        // Send an email alert
    }
}
```   

# 3. Subject Interface
The interface defines a method that all subjects must implement.
```java
// Subject Interface
public interface StockObservable {
    void Attach(NotificationAlertObserver observer);
    void Detach(NotificationAlertObserver observer);
    void Notify();
}
```

# 4. Concrete Subject
The concrete subject class implements the interface and provides its own implementation for the attach and detach methods.
```java
// Concrete Subject
public class StockObservableImpl implements StockObservable {

    private List<NotificationAlertObserver> observers = new ArrayList<>();
    private int stockNumber = 0;

    @Override
    public void Attach(NotificationAlertObserver observer) {
        observers.add(observer);
    }

    @Override    
    public void Detach(NotificationAlertObserver observer) {
        observers.remove(observer); 
    }
}
```

# 5. Client
The client class creates a concrete subject and attaches observers to it.
```java
// Client
public class Client {
    public static void main(String[] args) {
        StockObservableImpl stockObservable = new StockObservableImpl();
        NotificationAlertObserver emailAlertObserver = new EmailAlertObserverImpl();
        stockObservable.Attach(emailAlertObserver);
        stockObservable.Notify();   
    }
}
```

