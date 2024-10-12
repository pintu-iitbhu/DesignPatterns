package org.rcpky.Observable;

import org.rcpky.Observer.NotificationAlertObserver;

public interface StockObservable {
    public void attach(NotificationAlertObserver observer);
    public void detach(NotificationAlertObserver observer);
    public void notifyObservers();
    public int getStockNumber();
}
