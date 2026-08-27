package com.rcpky.observers.weather.observers;

import com.rcpky.observers.weather.observables.WeatherObservable;

public class CurrentConditionsDisplay implements WeatherObserver {
    //Shows the latest temperature and humidity.
    private double temperature;
    private double humidity;
    private WeatherObservable weatherObservable;

    public CurrentConditionsDisplay(WeatherObservable weatherObservable){
        this.weatherObservable = weatherObservable;
        weatherObservable.registerObserver(this);
    }

    @Override
    public void update() {
        temperature = weatherObservable.getTemperature();
        humidity = weatherObservable.getHumidity();

        System.out.println(
                "Current Conditions: "
                        + temperature
                        + "°C and "
                        + humidity
                        + "% humidity"
        );
    }
}
