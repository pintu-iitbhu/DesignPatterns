package com.rcpky.observers.weather.observables;

import com.rcpky.observers.weather.observers.WeatherObserver;

import java.util.HashSet;
import java.util.Set;

public class WeatherStation implements WeatherObservable{
    private double temperature;
    private double humidity;
    private double pressure;
    private Set<WeatherObserver> observers;

    public WeatherStation() {
        observers = new HashSet<>();
    }

    @Override
    public void registerObserver(WeatherObserver o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(WeatherObserver o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers() {
        observers.forEach(o -> o.update());
    }


    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }


    public void setMeasurements(
            double temperature,
            double humidity,
            double pressure
    ) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;

        notifyObservers();
    }
}
