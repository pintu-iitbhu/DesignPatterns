package com.rcpky.strategy.navigation;

public class PublicTransportRouteStrategy implements  RouteStrategy {
    @Override
    public void buildRoute(String source, String destination) {
        System.out.println("Building public transport route from " + source + " to " + destination);
    }
}
