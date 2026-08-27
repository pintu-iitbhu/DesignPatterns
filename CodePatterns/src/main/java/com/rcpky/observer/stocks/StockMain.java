package com.rcpky.observer.stocks;

import com.rcpky.observer.stocks.observables.Stock;
import com.rcpky.observer.stocks.observers.PortfolioTracker;
import com.rcpky.observer.stocks.observers.PriceAlert;
import com.rcpky.observer.stocks.observers.PriceDisplay;

public class StockMain {

    public  static void main(String[] args) {
        Stock stock = new Stock();
        stock.setName("AAPL");


        stock.attach(new PriceDisplay(stock));
        stock.attach(new PortfolioTracker(stock));
        stock.attach(new PriceAlert(stock));

        stock.setPrice(150.0);


    }
}
