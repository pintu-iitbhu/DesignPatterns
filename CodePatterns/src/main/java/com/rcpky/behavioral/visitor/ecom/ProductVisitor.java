package com.rcpky.behavioral.visitor.ecom;

public interface ProductVisitor {
    void visit(PhysicalProduct product);
    void visit(DigitalProduct product);
}
