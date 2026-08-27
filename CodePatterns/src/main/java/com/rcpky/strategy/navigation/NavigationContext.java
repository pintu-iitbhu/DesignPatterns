package com.rcpky.strategy.navigation;

public class NavigationContext {

    private RouteStrategy routeStrategy;

    public  NavigationContext(RouteStrategy routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public  RouteStrategy getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(RouteStrategy routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public void buildRoute(String source, String destination) {
        routeStrategy.buildRoute(source, destination);
    }


}
