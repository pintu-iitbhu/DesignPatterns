package com.rcpky.visitor.ecom;

public interface Product {
    void accept(ProductVisitor visitor);
}