package com.rcpky.decorator.coffeeorderingsystem;

public class SimpleCoffee implements  Coffee {

    private double price;
    private String description;

    public SimpleCoffee() {
        this.price = 100.0;
        this.description = "Simple Coffee";
    }

    @Override
    public double getPrice() {
        return this.price;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
