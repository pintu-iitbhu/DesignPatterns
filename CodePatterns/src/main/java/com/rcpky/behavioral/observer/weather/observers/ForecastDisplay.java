package com.rcpky.behavioral.observer.weather.observers;

import com.rcpky.behavioral.observer.weather.observables.WeatherObservable;

public class ForecastDisplay implements WeatherObserver {
    //Displays a weather forecast based on changes in pressure.
    private double previousPressure;
    private double currentPressure;
    private WeatherObservable weatherObservable;

    public ForecastDisplay(WeatherObservable weatherObservable){
        this.weatherObservable = weatherObservable;
        this.previousPressure = weatherObservable.getPressure();
        weatherObservable.registerObserver(this);
    }

    @Override
    public void update() {
        currentPressure = weatherObservable.getPressure();

        if (currentPressure > previousPressure) {

            System.out.println(
                    "Forecast: Improving weather expected."
            );

        } else if (currentPressure < previousPressure) {

            System.out.println(
                    "Forecast: Watch out for cooler, rainy weather."
            );

        } else {

            System.out.println(
                    "Forecast: More of the same."
            );
        }

        previousPressure = currentPressure;

    }

}
