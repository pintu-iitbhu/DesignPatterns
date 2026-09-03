package com.rcpky.visitor.documentprocessing;

import java.util.List;

public interface Document {
    public void accept(List<Operation> operations);
}
