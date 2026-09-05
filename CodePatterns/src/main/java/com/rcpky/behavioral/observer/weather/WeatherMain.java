package com.rcpky.behavioral.observer.weather;

import com.rcpky.behavioral.observer.weather.observables.WeatherStation;
import com.rcpky.behavioral.observer.weather.observers.CurrentConditionsDisplay;
import com.rcpky.behavioral.observer.weather.observers.ForecastDisplay;
import com.rcpky.behavioral.observer.weather.observers.StatisticsDisplay;
import com.rcpky.behavioral.observer.weather.observers.WeatherObserver;

public class WeatherMain {
    public static void main(String[] args) {
        WeatherStation weatherStation =
                new WeatherStation();

        WeatherObserver currentConditions =
                new CurrentConditionsDisplay(
                        weatherStation
                );

        WeatherObserver statistics =
                new StatisticsDisplay(
                        weatherStation
                );

        WeatherObserver forecast =
                new ForecastDisplay(
                        weatherStation
                );


//        weatherStation.registerObserver(
//                currentConditions
//        );
//
//        weatherStation.registerObserver(
//                statistics
//        );
//
//        weatherStation.registerObserver(
//                forecast
//        );


        System.out.println(
                "------ Update 1 ------"
        );

        weatherStation.setMeasurements(
                25.0,
                65.0,
                1012.0
        );


        System.out.println(
                "\n------ Update 2 ------"
        );

        weatherStation.setMeasurements(
                28.0,
                70.0,
                1015.0
        );


        System.out.println(
                "\n------ Update 3 ------"
        );

        weatherStation.setMeasurements(
                20.0,
                80.0,
                1005.0
        );


        System.out.println(
                "\n------ Removing Forecast Display ------"
        );

        weatherStation.removeObserver(
                forecast
        );


        System.out.println(
                "\n------ Update 4 ------"
        );

        weatherStation.setMeasurements(
                22.0,
                75.0,
                1010.0
        );
    }

}

