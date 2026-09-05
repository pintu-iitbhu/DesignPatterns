package com.rcpky.behavioral.visitor.ecom;

import java.math.BigDecimal;

public class DigitalProduct implements Product {

    private final String productId;
    private final String name;
    private final BigDecimal price;
    private final long fileSizeInMb;

    public DigitalProduct(String productId, String name, BigDecimal price, long fileSizeInMb) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.fileSizeInMb = fileSizeInMb;
    }

    @Override
    public void accept(ProductVisitor visitor) {

    }

    // constructor + getters
}
