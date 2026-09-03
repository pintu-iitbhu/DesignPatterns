package com.rcpky.visitor.ecom;

public interface ProductVisitor {
    void visit(PhysicalProduct product);
    void visit(DigitalProduct product);
}
