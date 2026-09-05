package com.rcpky.behavioral.strategy.navigation;

public class WalkingRouteStrategy implements  RouteStrategy {
    @Override
    public void buildRoute(String source, String destination) {
        System.out.println("Building walking route from " + source + " to " + destination);
    }
}
