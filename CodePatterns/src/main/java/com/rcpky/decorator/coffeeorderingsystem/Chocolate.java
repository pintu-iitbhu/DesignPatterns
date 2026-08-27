package com.rcpky.decorator.coffeeorderingsystem;

public class Chocolate implements  Coffee {
    private final Coffee coffee;

    public Chocolate(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double getPrice() {
        return coffee.getPrice() + 30;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Chocolate";
    }
}
