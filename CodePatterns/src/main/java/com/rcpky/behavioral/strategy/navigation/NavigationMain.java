package com.rcpky.behavioral.strategy.navigation;

public class NavigationMain {

    public  static void main(String[] args) {
        NavigationContext navigation =
                new NavigationContext(
                        new CarRouteStrategy()
                );

        navigation.buildRoute(
                "Home",
                "Office"
        );

        navigation.setRouteStrategy(
                new WalkingRouteStrategy()
        );

        navigation.buildRoute(
                "Office",
                "Restaurant"
        );

        navigation.setRouteStrategy(
                new PublicTransportRouteStrategy()
        );

        navigation.buildRoute(
                "Restaurant",
                "Airport"
        );
    }
}
