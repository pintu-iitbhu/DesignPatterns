package com.rcpky.structural.odercheckout.services;

import com.rcpky.creational.ordercreation.Address;
import com.rcpky.creational.ordercreation.OrderItem;
import com.rcpky.structural.odercheckout.ShippingResult;

import java.util.List;

public class ShippingService {

    public ShippingResult createShipment(
            String orderId,
            Address address,
            List<OrderItem> items
    ) {
        // Create shipment
        return new ShippingResult();
    }

    public void cancelShipment(String shipmentId) {
        // Cancel shipment
    }
}
