package com.rcpky.behavioral.observer.weather.observers;

import com.rcpky.behavioral.observer.weather.observables.WeatherObservable;

public class StatisticsDisplay implements WeatherObserver {
    //Tracks and displays:
    //
    //Average temperature
    //Minimum temperature
    //Maximum temperature
    private double minTemperature = Double.MAX_VALUE;
    private double maxTemperature = Double.MIN_VALUE;
    private double totalTemperature;

    private int numberOfReadings;
    private WeatherObservable weatherObservable;

    public StatisticsDisplay(WeatherObservable weatherObservable) {
        this.weatherObservable = weatherObservable;
        weatherObservable.registerObserver(this);
    }

    @Override
    public void update() {
        double currentTemperature =
                weatherObservable.getTemperature();

        totalTemperature += currentTemperature;
        numberOfReadings++;

        if (currentTemperature < minTemperature) {
            minTemperature = currentTemperature;
        }

        if (currentTemperature > maxTemperature) {
            maxTemperature = currentTemperature;
        }

        double averageTemperature =
                totalTemperature / numberOfReadings;

        System.out.println(
                "Statistics: Avg = "
                        + averageTemperature
                        + "°C, Min = "
                        + minTemperature
                        + "°C, Max = "
                        + maxTemperature
                        + "°C"
        );
    }
}
