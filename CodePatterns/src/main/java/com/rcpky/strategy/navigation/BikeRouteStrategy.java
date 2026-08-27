package com.rcpky.strategy.navigation;

public class BikeRouteStrategy implements  RouteStrategy {
    @Override
    public void buildRoute(String source, String destination) {
        System.out.println("Building bike route from " + source + " to " + destination);
    }
}
