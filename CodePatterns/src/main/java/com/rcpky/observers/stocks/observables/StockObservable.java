package com.rcpky.observers.stocks.observables;

import com.rcpky.observers.stocks.observers.Observer;

public interface StockObservable {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObserver();
    double getPrice();
    String getName();
}
