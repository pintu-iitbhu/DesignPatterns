package com.rcpky.behavioral.visitor.ecom;

public interface Product {
    void accept(ProductVisitor visitor);
}