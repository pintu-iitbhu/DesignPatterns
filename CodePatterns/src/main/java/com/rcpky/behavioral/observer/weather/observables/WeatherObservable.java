package com.rcpky.behavioral.observer.weather.observables;

import com.rcpky.behavioral.observer.weather.observers.WeatherObserver;

public interface WeatherObservable {
    void registerObserver(WeatherObserver o);
    void removeObserver(WeatherObserver o);
    void notifyObservers();
    double getTemperature();
    double getHumidity();
    double getPressure();

}
