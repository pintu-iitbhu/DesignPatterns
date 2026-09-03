package com.rcpky.visitor.documentprocessing.operations;

import com.rcpky.visitor.documentprocessing.Operation;
import com.rcpky.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.visitor.documentprocessing.documents.WordDocument;

public class StorageSizeOperation implements Operation {


    @Override
    public void apply(PdfDocument document) {
        System.out.println("Calculating storage size for PDF document");
    }

    @Override
    public void apply(WordDocument document) {
        System.out.println("Calculating storage size for Word document");
    }

    @Override
    public void apply(ExcelDocument document) {
        System.out.println("Calculating storage size for Excel document");
    }
}
