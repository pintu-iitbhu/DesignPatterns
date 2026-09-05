package com.rcpky.behavioral.visitor.documentprocessing.operations;

import com.rcpky.behavioral.visitor.documentprocessing.Operation;
import com.rcpky.behavioral.visitor.documentprocessing.documents.ExcelDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.PdfDocument;
import com.rcpky.behavioral.visitor.documentprocessing.documents.WordDocument;

public class MetadataExportOperation implements Operation {
    @Override
    public void apply(PdfDocument document) {
        System.out.println("Exporting metadata for PDF document");
    }

    @Override
    public void apply(WordDocument document) {
        System.out.println("Exporting metadata for Word document");
    }

    @Override
    public void apply(ExcelDocument document) {
        System.out.println("Exporting metadata for Excel document");
    }
}
