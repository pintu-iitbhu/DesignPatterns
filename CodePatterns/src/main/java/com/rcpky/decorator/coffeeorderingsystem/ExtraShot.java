package com.rcpky.decorator.coffeeorderingsystem;

public class ExtraShot implements  Coffee {

    private final Coffee coffee;

    public ExtraShot(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double getPrice() {
        return coffee.getPrice() + 40;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", Extra Shot";
    }
}
