package com.rcpky.behavioral.observer.stocks.observers;

import com.rcpky.behavioral.observer.stocks.observables.StockObservable;

public class PriceAlert implements  Observer{
    private StockObservable stockObservable;

    public PriceAlert(StockObservable stockObservable) {
        this.stockObservable = stockObservable;
    }

    @Override
    public void update() {
        System.out.println("PriceAlert: Price of " + stockObservable.getName() + " is " + stockObservable.getPrice());
    }
}
