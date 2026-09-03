package com.rcpky.visitor.documentprocessing.documents;

import com.rcpky.visitor.documentprocessing.Document;
import com.rcpky.visitor.documentprocessing.Operation;

import java.util.List;

public class WordDocument implements Document {
    @Override
    public void accept(List<Operation> operations) {
        System.out.println("accepting word documents");
        operations.forEach(operation -> operation.apply(this));
    }
}
