package com.rcpky.structural.odercheckout.services;

import com.rcpky.creational.ordercreation.OrderItem;

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
