package com.rcpky.behavioral.observer.stocks.observables;

import com.rcpky.behavioral.observer.stocks.observers.Observer;

public interface StockObservable {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObserver();
    double getPrice();
    String getName();
}
