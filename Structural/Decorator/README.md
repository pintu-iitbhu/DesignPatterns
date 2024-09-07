# Decorator Design Pattern

The **Decorator Design Pattern** is a structural design pattern that allows behavior to be added to individual objects, dynamically, without affecting the behavior of other objects from the same class. It’s a flexible alternative to subclassing for extending functionality.

# Key Concepts:
1. Component Interface: This defines the common interface for both the concrete component and decorators.
2. Concrete Component: The class to which additional behavior will be added dynamically.
3. Decorator Class: A class that implements the component interface and contains a reference to a component object (the object being decorated).
4. Concrete Decorators: These extend the functionality of the component by adding new behaviors.

# Example: Decorator Pattern in Java

Let’s say we have a basic Coffee class, and we want to add different kinds of add-ons to the coffee (like milk, sugar, etc.), but we want to do this dynamically, without modifying the Coffee class directly.

# 1. Component Interface
The base interface for the coffee object.
```java
public interface Coffee {
    String getDescription();
    double getCost();
}
```

# 2. Concrete Component
The core coffee object.
```java
public class SimpleCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "Simple Coffee";
    }

    @Override
    public double getCost() {
        return 5.0;
    }
}
```

# 3. Decorator Class
The abstract decorator class that implements the Coffee interface and wraps a Coffee object.
```java
public abstract class CoffeeDecorator implements Coffee {
    protected Coffee decoratedCoffee;

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }
}
```
# 4. Concrete Decorators
Now we create concrete decorators that extend the CoffeeDecorator class to add additional behavior.

```java
// Adds Milk to the coffee
public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + ", Milk";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + 1.5;
    }
}

// Adds Sugar to the coffee
public class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + ", Sugar";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + 0.5;
    }
}
```

# 5. Client Code
The client can now dynamically add decorators to the Coffee object.

```java
public class DecoratorPatternDemo {
    public static void main(String[] args) {
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " $" + coffee.getCost());
        
        // Add Milk to the coffee
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " $" + coffee.getCost());
        
        // Add Sugar to the coffee
        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " $" + coffee.getCost());
    }
}

```

# How it Works:
- The CoffeeDecorator wraps the Coffee object, allowing dynamic extension of functionality.
- The concrete decorators (MilkDecorator and SugarDecorator) add their specific behavior (like adding milk or sugar) without modifying the base Coffee class or creating a large number of subclasses.
- The client can keep adding decorators as needed, which results in cumulative behavior (e.g., adding both milk and sugar).


# Applicability
- Use the Decorator pattern when you need to be able to assign extra behaviors to objects at runtime without breaking the code that uses these objects.
- Use the pattern when it’s awkward or not possible to extend an object’s behavior using inheritance.

# When to Use the Decorator Pattern:
- When you want to add responsibilities to individual objects dynamically and transparently.
- When extending behavior by subclassing is impractical or leads to an explosion of subclasses.
- When you want to avoid modifying the existing class and instead prefer a composition approach.