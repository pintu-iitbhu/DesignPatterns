package com.rcpky.behavioral.visitor.documentprocessing.operations;

import com.rcpky.behavioral.visitor.documentprocessing.Operation;
import com.rcpky.behavioral.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.WordDocument;

public class SecurityScanOperation implements Operation {
    @Override
    public void apply(PdfDocument document) {
        System.out.println("Scanning PDF document for security");
    }

    @Override
    public void apply(WordDocument document) {
        System.out.println("Scanning word document for security");
    }

    @Override
    public void apply(ExcelDocument document) {
        System.out.println("Scanning excel document for security");
    }
}
