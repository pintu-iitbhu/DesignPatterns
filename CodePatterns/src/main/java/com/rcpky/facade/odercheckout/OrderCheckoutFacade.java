package com.rcpky.facade.odercheckout;

import com.rcpky.builder.ordercreation.Address;
import com.rcpky.builder.ordercreation.OrderItem;
import com.rcpky.facade.odercheckout.services.InventoryService;
import com.rcpky.facade.odercheckout.services.NotificationService;
import com.rcpky.facade.odercheckout.services.PaymentService;
import com.rcpky.facade.odercheckout.services.ShippingService;

import java.math.BigDecimal;
import java.util.List;

public class OrderCheckoutFacade {


    private NotificationService notificationService;
    private PaymentService paymentService;
    private ShippingService shippingService;
    private InventoryService inventoryService;

    public OrderCheckoutFacade(NotificationService notificationService,
                               PaymentService paymentService,
                               ShippingService shippingService,
                               InventoryService inventoryService) {
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
        this.inventoryService = inventoryService;
    }

    public void checkout(String orderId, String customerId, List<OrderItem> orderItems) {

        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }

        inventoryService.releaseItems(orderId);

        paymentService.processPayment(customerId,new BigDecimal(234));

        shippingService.createShipment(orderId, new Address("123 Main St", "Anytown"), orderItems);

        notificationService.sendOrderConfirmation(customerId, orderId);

    }
}
