package com.rcpky.observer.stocks.observers;

import com.rcpky.observer.stocks.observables.StockObservable;

public class PortfolioTracker implements  Observer{

    private double portfolioValue;
    private StockObservable stockObservable;

    public PortfolioTracker(StockObservable stockObservable) {
        this.stockObservable = stockObservable;
    }

    @Override
    public void update() {
        portfolioValue = stockObservable.getPrice();
        System.out.println("PortfolioTracker: Portfolio value updated to " + portfolioValue);

    }
}
