package com.rcpky.observers.stocks.observables;

import com.rcpky.observers.stocks.observers.Observer;
import com.rcpky.observers.stocks.observers.PriceAlert;


import java.util.ArrayList;
import java.util.List;


public class Stock implements StockObservable{
    private String name;
    private double price;
    private double alertPrice;
    private List<Observer> observers;
    public Stock(){
        this.observers = new ArrayList<>();
        this.alertPrice = 100.0;
    }


    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObserver() {
        for(Observer observer : observers){
            if (this.price<this.alertPrice || observer instanceof PriceAlert){
                continue;
            }
            observer.update();
        }

    }

    @Override
    public double getPrice() {
        return this.price;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setPrice(double price){
        this.price = price;
        notifyObserver();
    }

    public  void setName(String name) {
        this.name = name;
    }
}
