package com.rcpky.behavioral.strategy.navigation;

public class CarRouteStrategy implements  RouteStrategy {
    @Override
    public void buildRoute(String source, String destination) {
        System.out.println("Building car route from " + source + " to " + destination);
    }
}
