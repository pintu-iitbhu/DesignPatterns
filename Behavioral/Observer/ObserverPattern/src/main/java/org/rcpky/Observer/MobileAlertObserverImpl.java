package org.rcpky.Observer;

import org.rcpky.Observable.StockObservable;

public class MobileAlertObserverImpl implements NotificationAlertObserver{
    private String mobileUserName;
    private StockObservable stockObservable;
    public MobileAlertObserverImpl(StockObservable stockObservable, String mobileUserName){
        this.stockObservable = stockObservable;
        this.mobileUserName=mobileUserName;
    }

    @Override
    public void Update() {
        sendMessage(mobileUserName, "hurry up for the mobile", this.stockObservable.getStockNumber());
    }

    private void sendMessage(String userName, String message, int stockNumber) {
        System.out.println(userName + " : " + message + " : " + stockNumber);
    }
}
