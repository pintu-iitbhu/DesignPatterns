package com.rcpky.behavioral.visitor.documentprocessing.operations;

import com.rcpky.behavioral.visitor.documentprocessing.Operation;
import com.rcpky.behavioral.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.WordDocument;

public class PreviewOperation implements Operation {

    @Override
    public void apply(PdfDocument document) {
        System.out.println("Previewing PDF document");
    }

    @Override
    public void apply(WordDocument document) {
        System.out.println("Previewing word document");
    }

    @Override
    public void apply(ExcelDocument document) {
        System.out.println("Previewing excel document" );
    }
}
