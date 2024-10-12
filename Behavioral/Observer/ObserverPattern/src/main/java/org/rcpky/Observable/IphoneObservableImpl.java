package org.rcpky.Observable;

import org.rcpky.Observer.NotificationAlertObserver;

import java.util.ArrayList;
import java.util.List;

public class IphoneObservableImpl implements StockObservable{
    private List<NotificationAlertObserver> observers= new ArrayList<>(); // <1>
    private int stockNumber=0;
    @Override
    public void attach(NotificationAlertObserver observer) {
        if (observers == null) {
            observers = List.of(observer);
        } else {
            observers.add(observer);
        }
    }

    @Override
    public void detach(NotificationAlertObserver observer) {
        if(observers != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers() {
        for (NotificationAlertObserver observer : observers) {
            observer.Update();
        }

    }

    public void setStockNumber(int number) {
        if(this.stockNumber==0){
            notifyObservers();
        }
        this.stockNumber += number;
    }
    @Override
    public int getStockNumber(){
        return this.stockNumber;
    }
}
