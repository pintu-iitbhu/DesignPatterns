package org.rcpky.Observer;

import org.rcpky.Observable.StockObservable;

public class EmailAlertObserverImpl implements NotificationAlertObserver{
    private String email;
    private StockObservable stockObservable;

    public EmailAlertObserverImpl(StockObservable stockObservable, String email){
        this.stockObservable = stockObservable;
        this.email=email;

    }

    @Override
    public void Update() {
        sendEmail(email,"Product in the stock, Hurry up",stockObservable.getStockNumber());
        System.out.println("Email Alert: ");
    }

    private void sendEmail(String email, String subject, int number){
        System.out.println(email + " " + subject + " " + number);
    }

}
