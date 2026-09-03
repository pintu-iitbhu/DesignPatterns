package com.rcpky.visitor.ecom;

import java.math.BigDecimal;

public class PhysicalProduct implements Product {

    private final String productId;
    private final String name;
    private final BigDecimal price;
    private final double weight;

    public PhysicalProduct(String productId, String name, BigDecimal price, double weight) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.weight = weight;
    }

    // constructor + getters

    @Override
    public void accept(ProductVisitor visitor) {
        // implement
    }
}