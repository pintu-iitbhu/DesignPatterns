package org.rcpky;

import org.rcpky.clients.NormalVehicle;
import org.rcpky.clients.SportsVehicle;

/**
 * Hello world!
 *
 */
public class StrategyPattern
{
    public static void main( String[] args )
    {
        NormalVehicle normalVehicle = new NormalVehicle();
        normalVehicle.Drive();
        SportsVehicle sportsVehicle = new SportsVehicle();
        sportsVehicle.Drive();
    }
}
