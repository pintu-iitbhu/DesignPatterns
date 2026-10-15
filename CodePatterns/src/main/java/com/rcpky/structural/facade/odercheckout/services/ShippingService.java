package com.rcpky.structural.facade.odercheckout.services;

import com.rcpky.creational.builder.ordercreation.Address;
import com.rcpky.creational.builder.ordercreation.OrderItem;
import com.rcpky.structural.facade.odercheckout.ShippingResult;

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
