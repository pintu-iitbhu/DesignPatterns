package com.rcpky.observers.stocks;

import com.rcpky.observers.stocks.observables.Stock;
import com.rcpky.observers.stocks.observers.PortfolioTracker;
import com.rcpky.observers.stocks.observers.PriceAlert;
import com.rcpky.observers.stocks.observers.PriceDisplay;

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
