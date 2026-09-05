package com.rcpky.structural.decorator.coffeeorderingsystem;

public class Milk implements Coffee{
    private final Coffee coffee;

    public Milk(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double getPrice() {
        return coffee.getPrice() + 20;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Milk";
    }
}
