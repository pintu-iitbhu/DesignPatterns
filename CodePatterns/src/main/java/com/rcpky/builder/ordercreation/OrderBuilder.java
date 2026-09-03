package com.rcpky.builder.ordercreation;


// =========================
// BUILDER
// =========================

import java.util.*;

public class OrderBuilder {

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

    // Required fields
    private String orderId;
    private String customerId;
    private final List<OrderItem> items = new ArrayList<>();

    // Optional fields
    private Address shippingAddress;
    private Address billingAddress;
    private String couponCode;
    private String giftMessage;

    // Default value
    private Priority priority = Priority.NORMAL;

    private String deliveryInstructions;

    private final Map<String, String> metadata = new HashMap<>();


    public OrderBuilder orderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderBuilder customerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public OrderBuilder addItem(OrderItem item) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "Order item cannot be null"
            );
        }

        this.items.add(item);

        return this;
    }

    public OrderBuilder items(List<OrderItem> items) {

        if (items == null) {
            throw new IllegalArgumentException(
                    "Items cannot be null"
            );
        }

        this.items.clear();
        this.items.addAll(items);

        return this;
    }

    public OrderBuilder shippingAddress(
            Address shippingAddress
    ) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    public OrderBuilder billingAddress(
            Address billingAddress
    ) {
        this.billingAddress = billingAddress;
        return this;
    }

    public OrderBuilder couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    public OrderBuilder giftMessage(String giftMessage) {
        this.giftMessage = giftMessage;
        return this;
    }

    public OrderBuilder priority(Priority priority) {

        this.priority = Objects.requireNonNull(
                priority,
                "Priority cannot be null"
        );

        return this;
    }

    public OrderBuilder deliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions =
                deliveryInstructions;

        return this;
    }

    public OrderBuilder metadata(
            String key,
            String value
    ) {

        if (key == null || value == null) {
            throw new IllegalArgumentException(
                    "Metadata key and value cannot be null"
            );
        }

        this.metadata.put(key, value);

        return this;
    }


    // =========================
    // VALIDATION + BUILD
    // =========================

    public Order build() {

        validate();

        return new Order(this);
    }

    private void validate() {

        if (orderId == null
                || orderId.isEmpty()) {

            throw new IllegalStateException(
                    "orderId is required"
            );
        }

        if (customerId == null
                || customerId.isEmpty()) {

            throw new IllegalStateException(
                    "customerId is required"
            );
        }

        if (items.isEmpty()) {

            throw new IllegalStateException(
                    "Order must contain at least one item"
            );
        }

        if (items.stream()
                .anyMatch(Objects::isNull)) {

            throw new IllegalStateException(
                    "Order items cannot contain null values"
            );
        }
    }
}
