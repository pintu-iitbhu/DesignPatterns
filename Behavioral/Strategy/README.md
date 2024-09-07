# Strategy Design Pattern
The **Strategy Design Pattern** is a behavioral design pattern that allows you to define a family of algorithms, encapsulate each one as a separate class, and make them interchangeable. The pattern lets the algorithm vary independently from the clients that use it.

# Key Concepts:
1. **Strategy Interface**: Declares an interface common to all supported algorithms.
2. **Concrete Strategies**: Implement different variations of the algorithm defined by the strategy interface.
3. **Context**: Maintains a reference to a strategy object and delegates the execution of the algorithm to that strategy.

# Benefits:
- **Open/Closed Principle**: You can introduce new strategies without changing existing code.
- **Eliminates Conditional Statements**: By encapsulating the algorithms, you avoid having complex if-else or switch-case statements.
- **Interchangeable Strategies**: You can easily switch between different algorithms at runtime.

# Example: Strategy Pattern in Java
Imagine we are building a navigation system that calculates routes based on different strategies (e.g., walking, driving, cycling). We want the ability to switch between these strategies dynamically

# 1. Strategy Interface
The interface defines a method that all strategies must implement.
```java
// Strategy Interface
public interface RouteStrategy {
    void calculateRoute(String startPoint, String endPoint);
}
```

# 2. Concrete Strategies
Each concrete strategy implements the RouteStrategy interface, providing different algorithms for calculating the route.
```java
// Concrete Strategy - Driving
public class DrivingStrategy implements RouteStrategy {
    @Override
    public void calculateRoute(String startPoint, String endPoint) {
        System.out.println("Calculating route for driving from " + startPoint + " to " + endPoint);
    }
}

// Concrete Strategy - Walking
public class WalkingStrategy implements RouteStrategy {
    @Override
    public void calculateRoute(String startPoint, String endPoint) {
        System.out.println("Calculating route for walking from " + startPoint + " to " + endPoint);
    }
}

// Concrete Strategy - Cycling
public class CyclingStrategy implements RouteStrategy {
    @Override
    public void calculateRoute(String startPoint, String endPoint) {
        System.out.println("Calculating route for cycling from " + startPoint + " to " + endPoint);
    }
}

```

# 3. Context Class
The context class maintains a reference to a RouteStrategy object and uses it to execute the algorithm.
```java
// Context
public class NavigationContext {
    private RouteStrategy strategy;

    public void setStrategy(RouteStrategy strategy) {
        this.strategy = strategy;
    }

    public void executeRoute(String startPoint, String endPoint) {
        strategy.calculateRoute(startPoint, endPoint);
    }
}
```

# 4. Client Code
The client can change strategies dynamically by setting different algorithms for route calculation.
```java

public class StrategyPatternDemo {
    public static void main(String[] args) {
        NavigationContext context = new NavigationContext();

        // Set strategy to Driving
        context.setStrategy(new DrivingStrategy());
        context.executeRoute("Home", "Office");

        // Set strategy to Walking
        context.setStrategy(new WalkingStrategy());
        context.executeRoute("Home", "Park");

        // Set strategy to Cycling
        context.setStrategy(new CyclingStrategy());
        context.executeRoute("Home", "Gym");
    }
}
```
# How it Works:
- The RouteStrategy interface defines the method that all strategies must implement (calculateRoute).
- The concrete strategy classes (DrivingStrategy, WalkingStrategy, CyclingStrategy) implement the different routing algorithms.
- The NavigationContext class is the context that maintains a reference to the current strategy and delegates the route calculation to that strategy.
- The client code can dynamically change the strategy by calling setStrategy on the context and then executing the route calculation with executeRoute.

# When to Use the Strategy Pattern:
- When you have multiple algorithms for a specific task, and you want to choose between them at runtime.
- When you want to avoid complex conditional statements (if-else or switch-case) to select different behaviors.
- When you want to make algorithms interchangeable and easily extensible without modifying client code.

# Applicability
 - Use the Strategy pattern when you want to use different variants of an algorithm within an object and be able to switch from one algorithm to another during runtime.
 -  Use the Strategy when you have a lot of similar classes that only differ in the way they execute some behavior.
 -  Use the pattern to isolate the business logic of a class from the implementation details of algorithms that may not be as important in the context of that logic.
 - Use the pattern when your class has a massive conditional statement that switches between different variants of the same algorithm.