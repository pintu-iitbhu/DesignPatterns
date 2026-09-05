package com.rcpky.behavioral.visitor.documentprocessing.documents;

import com.rcpky.behavioral.visitor.documentprocessing.Document;
import com.rcpky.behavioral.visitor.documentprocessing.Operation;

import java.util.List;

public class ExcelDocument implements Document {

    @Override
    public void accept(List<Operation> operations) {
        System.out.println("Accepting Excel Document");
        operations.forEach(operation -> operation.apply(this));

    }
}
