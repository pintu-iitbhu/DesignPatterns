package com.rcpky.builder.ordercreation;

import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public final class Order {

    // Required
    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;

    // Optional
    private final Address shippingAddress;
    private final Address billingAddress;
    private final String couponCode;
    private final String giftMessage;
    private final Priority priority;
    private final String deliveryInstructions;
    private final Map<String, String> metadata;

    // Private constructor
    // Only Builder can create an Order
    public Order(OrderBuilder builder) {

        this.orderId = builder.getOrderId();
        this.customerId = builder.getCustomerId();

        // Defensive copy + immutable collection
        this.items = Collections.unmodifiableList(
                new ArrayList<>(builder.getItems())
        );

        this.shippingAddress = builder.getShippingAddress();
        this.billingAddress = builder.getBillingAddress();
        this.couponCode = builder.getCouponCode();
        this.giftMessage = builder.getGiftMessage();

        this.priority = builder.getPriority();

        this.deliveryInstructions =
                builder.getDeliveryInstructions();

        // Defensive copy + immutable collection
        this.metadata = Collections.unmodifiableMap(
                new HashMap<>(builder.getMetadata())
        );
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    // Getters

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getGiftMessage() {
        return giftMessage;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }



}
