package com.rcpky.decorator.coffeeorderingsystem;

public class CoffeeShop {

    public static void main(String[] args) {
        // Simple Coffee
        Coffee coffee1 = new SimpleCoffee();

        System.out.println(coffee1.getDescription());
        System.out.println("₹" + coffee1.getPrice());


        // Simple Coffee + Milk
        Coffee coffee2 = new Milk(
                new SimpleCoffee()
        );

        System.out.println("\n" + coffee2.getDescription());
        System.out.println("₹" + coffee2.getPrice());


        // Simple Coffee + Milk + Chocolate + Extra Shot
        Coffee coffee3 = new ExtraShot(
                new Chocolate(
                        new Milk(
                                new SimpleCoffee()
                        )
                )
        );

        System.out.println("\n" + coffee3.getDescription());
        System.out.println("₹" + coffee3.getPrice());
    }
}
