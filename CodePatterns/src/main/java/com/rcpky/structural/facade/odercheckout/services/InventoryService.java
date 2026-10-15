package com.rcpky.structural.facade.odercheckout.services;

import com.rcpky.creational.builder.ordercreation.OrderItem;

import java.util.List;

public class InventoryService {

    public boolean reserveItems(String orderId, List<OrderItem> items) {
        // Check and reserve inventory
        return true;
    }

    public void releaseItems(String orderId) {
        // Release reserved inventory
    }
}
