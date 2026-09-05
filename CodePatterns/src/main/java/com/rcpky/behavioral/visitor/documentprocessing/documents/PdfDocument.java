package com.rcpky.behavioral.visitor.documentprocessing.documents;

import com.rcpky.behavioral.visitor.documentprocessing.Document;
import com.rcpky.behavioral.visitor.documentprocessing.Operation;

import java.util.List;

public class PdfDocument implements Document {
    @Override
    public void accept(List<Operation> operations) {
        System.out.println("accepting pdf documents");
        operations.forEach(operation ->  operation.apply(this));
    }
}
