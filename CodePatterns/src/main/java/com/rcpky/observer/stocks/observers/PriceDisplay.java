package com.rcpky.observer.stocks.observers;

import com.rcpky.observer.stocks.observables.StockObservable;

public class PriceDisplay implements Observer{

    StockObservable stockObservable;
    public PriceDisplay(StockObservable observable){
        this.stockObservable = observable;
    }
    @Override
    public void update() {
        System.out.println("PriceDisplay: Price of " + stockObservable.getName() + " is " + stockObservable.getPrice());
    }
}
