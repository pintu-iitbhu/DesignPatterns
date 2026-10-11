package com.rcpky.creational.ordercreation;

public class Demo {

    public  static void main(String[] args) {
        OrderItem item1 =
                new OrderItem("PRODUCT-101", 2);

        OrderItem item2 =
                new OrderItem("PRODUCT-202", 1);

        Address address =
                new Address(
                        "MG Road",
                        "Bengaluru"
                );


        Order order = Order.builder()
                .orderId("ORD-1001")
                .customerId("CUST-101")
                .addItem(item1)
                .addItem(item2)
                .shippingAddress(address)
                .couponCode("FESTIVAL10")
                .priority(Priority.HIGH)
                .metadata("source", "mobile-app")
                .metadata("campaign", "diwali")
                .build();

        System.out.println(order);
    }
}
