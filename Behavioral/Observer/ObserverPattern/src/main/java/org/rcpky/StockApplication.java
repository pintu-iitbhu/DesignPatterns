package org.rcpky;

import org.rcpky.Observable.IphoneObservableImpl;
import org.rcpky.Observer.EmailAlertObserverImpl;
import org.rcpky.Observer.MobileAlertObserverImpl;

/**
 * Hello world!
 *
 */
public class StockApplication
{
    public static void main( String[] args )
    {

        System.out.println( "Hello World!" );

        IphoneObservableImpl iphoneObservable = new IphoneObservableImpl();

        EmailAlertObserverImpl emailAlertObserver = new EmailAlertObserverImpl(iphoneObservable, "w5k7H@example.com");
        EmailAlertObserverImpl emailAlertObserver2 = new EmailAlertObserverImpl(iphoneObservable, "w5k7H@example.com");
        MobileAlertObserverImpl mobileAlertObserver = new MobileAlertObserverImpl(iphoneObservable, "mobile");
        MobileAlertObserverImpl mobileAlertObserver2 = new MobileAlertObserverImpl(iphoneObservable, "mobile");


        iphoneObservable.attach(emailAlertObserver);
        iphoneObservable.attach(emailAlertObserver2);
        iphoneObservable.attach(mobileAlertObserver);
        iphoneObservable.attach(mobileAlertObserver2);

        iphoneObservable.setStockNumber(10);
    }
}
