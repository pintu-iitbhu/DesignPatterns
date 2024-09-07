# Example: Decorator Pattern in Go

Let’s say we have a basic Coffee class, and we want to add different kinds of add-ons to the coffee (like milk, sugar, etc.), but we want to do this dynamically, without modifying the Coffee class directly.

# 1. Component Interface
The base interface for the coffee object.
```go
type Coffee interface {
    GetDescription() string
    GetCost() float64
}
```

# 2. Concrete Component
The core coffee object.
```go
// SimpleCoffee is the basic coffee with no add-ons.
type SimpleCoffee struct{}

// GetDescription returns the description of SimpleCoffee.
func (c *SimpleCoffee) GetDescription() string {
    return "Simple Coffee"
}

// GetCost returns the cost of SimpleCoffee.
func (c *SimpleCoffee) GetCost() float64 {
    return 5.0
}

```

# 3. Decorator Class
The abstract decorator class that implements the Coffee interface and wraps a Coffee object.
```go
// CoffeeDecorator wraps a Coffee object and implements the Coffee interface.
type CoffeeDecorator struct {
    decoratedCoffee Coffee
}

// GetDescription calls the GetDescription method on the wrapped Coffee object.
func (d *CoffeeDecorator) GetDescription() string {
    return d.decoratedCoffee.GetDescription()
}

// GetCost calls the GetCost method on the wrapped Coffee object.
func (d *CoffeeDecorator) GetCost() float64 {
    return d.decoratedCoffee.GetCost()
}

```
# 4. Concrete Decorators
Concrete decorators (MilkDecorator and SugarDecorator) extend the CoffeeDecorator to add additional functionality. They dynamically modify the description and cost of the Coffee object.

## Milk Decorator
Adds milk to the coffee
```go
// MilkDecorator adds milk to the coffee.
type MilkDecorator struct {
    CoffeeDecorator
}

// GetDescription adds "Milk" to the coffee description.
func (d *MilkDecorator) GetDescription() string {
    return d.decoratedCoffee.GetDescription() + ", Milk"
}

// GetCost adds the cost of milk to the coffee.
func (d *MilkDecorator) GetCost() float64 {
    return d.decoratedCoffee.GetCost() + 1.5
}
```
## Sugar Decorator
Adds sugar to the coffee.

```go
// SugarDecorator adds sugar to the coffee.
type SugarDecorator struct {
    CoffeeDecorator
}

// GetDescription adds "Sugar" to the coffee description.
func (d *SugarDecorator) GetDescription() string {
    return d.decoratedCoffee.GetDescription() + ", Sugar"
}

// GetCost adds the cost of sugar to the coffee.
func (d *SugarDecorator) GetCost() float64 {
    return d.decoratedCoffee.GetCost() + 0.5
}
```

# 5. Client Code
The client can now dynamically add decorators to the Coffee object.

```go
func main() {
    var coffee Coffee = &SimpleCoffee{}
    fmt.Println(coffee.GetDescription(), "$", coffee.GetCost())

    // Add Milk to the coffee
    coffee = &MilkDecorator{
        CoffeeDecorator{decoratedCoffee: coffee},
    }
    fmt.Println(coffee.GetDescription(), "$", coffee.GetCost())

    // Add Sugar to the coffee
    coffee = &SugarDecorator{
        CoffeeDecorator{decoratedCoffee: coffee},
    }
    fmt.Println(coffee.GetDescription(), "$", coffee.GetCost())
}
```

# How it Works:
- Component Interface (Coffee): Defines the core functionality (getting description and cost) that all coffee types must implement.
- Concrete Component (SimpleCoffee): Implements the base behavior for a simple coffee.
- Decorator Class (CoffeeDecorator): A wrapper for the Coffee interface that allows adding behavior dynamically. It forwards calls to the decorated coffee object.
- Concrete Decorators (MilkDecorator, SugarDecorator): Extend the CoffeeDecorator to add specific functionality (like adding milk or sugar), without modifying the base coffee class.
- Client Code: The client can dynamically add decorators to the coffee object to extend its behavior (e.g., adding both milk and sugar)


